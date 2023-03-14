package no.elg.infiniteBootleg.world

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.ObjectSet
import com.google.common.base.Preconditions
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.events.BlockChangedEvent
import no.elg.infiniteBootleg.events.api.EventListener
import no.elg.infiniteBootleg.events.api.EventManager.dispatchEvent
import no.elg.infiniteBootleg.events.api.EventManager.registerListener
import no.elg.infiniteBootleg.events.chunks.ChunkLightUpdatedEvent
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2i
import no.elg.infiniteBootleg.server.broadcastToInView
import no.elg.infiniteBootleg.server.clientBoundBlockUpdate
import no.elg.infiniteBootleg.server.serverBoundBlockUpdate
import no.elg.infiniteBootleg.util.chunkOffset
import no.elg.infiniteBootleg.util.chunkToWorld
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.directionTo
import no.elg.infiniteBootleg.util.isInsideChunk
import no.elg.infiniteBootleg.util.isNeighbor
import no.elg.infiniteBootleg.util.stringifyChunkToWorld
import no.elg.infiniteBootleg.world.blocks.TickingBlock
import no.elg.infiniteBootleg.world.box2d.ChunkBody
import no.elg.infiniteBootleg.world.render.ClientWorldRender
import no.elg.infiniteBootleg.world.world.World
import org.jetbrains.annotations.Contract
import java.util.concurrent.CancellationException
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.concurrent.GuardedBy

@Suppress("GDXKotlinUnsafeIterator")
class ChunkImpl(
  override val world: World,
  override val chunkX: Int,
  override val chunkY: Int
) : Chunk {

  val currentUpdateId = AtomicInteger()

  override val blocks: Array<Array<Block?>> = Array(Chunk.CHUNK_SIZE) { arrayOfNulls(Chunk.CHUNK_SIZE) }
  private val blockLights = Array(Chunk.CHUNK_SIZE) { x -> Array(Chunk.CHUNK_SIZE) { y -> BlockLight(this, x, y) } }

  private val tickingBlocks = TickingBlocks()
  override val chunkBody: ChunkBody = ChunkBody(this)

  @Volatile
  private var lightUpdater: ScheduledFuture<*>? = null
  private val tasks = ObjectSet<ForkJoinTask<*>>(Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE, 0.99f)

  /**
   * if texture/allair needs to be updated
   *
   * Use [dirty] to mark chunk as dirty
   * */
  @get:Synchronized
  @Volatile
  override var isDirty = true
    private set

  /**
   * if this chunk should be prioritized to be updated
   */
  private var prioritize: Boolean = false

  /**
   * If the chunk has been modified since loaded
   */
  @Volatile
  private var modified: Boolean = false

  /**
   * Whether this chunk is allowed to unload.
   * Sometimes we do not want to unload specific chunks, for example if there is a player in that chunk
   *  or it is the spawn chunk
   */
  @Volatile
  private var allowUnload: Boolean = true

  /**
   * If the chunk is still being initialized, meaning not all blocks are in [.blocks] and
   * [tickingBlocks] does not contain all blocks it should
   */
  @Volatile
  private var initializing: Boolean = true

  @Volatile
  private var allAir: Boolean = false

  @Volatile
  private var disposed: Boolean = false

  /**
   * @return The last tick this chunk's texture was pulled
   */
  @Volatile
  override var lastViewedTick: Long = 0
    private set

  private val fboLock = Any()

  @GuardedBy("fboLock")
  private var fboRegion: TextureRegion? = null

  @GuardedBy("fboLock")
  private var fbo: FrameBuffer? = null

  private val updateChunkLightEventListener = EventListener { (chunk, localX1, localY1): ChunkLightUpdatedEvent ->
    if (this.isNeighbor(
        chunk
      )
    ) {
      val dir = chunk.directionTo(this)
      val localX = (localX1 + dir.dx * World.LIGHT_SOURCE_LOOK_BLOCKS).toInt()
      val localY = (localY1 + dir.dy * World.LIGHT_SOURCE_LOOK_BLOCKS).toInt()
      val xCheck = when (dir.dx) {
        -1 -> localX < 0
        0 -> true
        1 -> localX > Chunk.CHUNK_SIZE
        else -> false
      }
      val yCheck = when (dir.dy) {
        -1 -> localY < 0
        0 -> true
        1 -> localY > Chunk.CHUNK_SIZE
        else -> false
      }
      if (xCheck && yCheck) {
        updateBlockLights(
          localX.chunkOffset(),
          localY.chunkOffset(),
          false
        )
      }
    }
  }

  @Contract("_, _, !null, _, _, _ -> !null; _, _, null, _, _, _ -> null")
  override fun setBlock(
    localX: Int,
    localY: Int,
    material: Material?,
    updateTexture: Boolean,
    prioritize: Boolean,
    sendUpdatePacket: Boolean
  ): Block? {
    val block = material?.createBlock(world, this, localX, localY)
    return setBlock(
      localX = localX,
      localY = localY,
      block = block,
      updateTexture = updateTexture,
      prioritize = prioritize,
      sendUpdatePacket = sendUpdatePacket
    )
  }

  override fun removeBlock(localX: Int, localY: Int, updateTexture: Boolean, prioritize: Boolean, sendUpdatePacket: Boolean) {
    setBlock(
      localX = localX,
      localY = localY,
      block = null as Block?,
      updateTexture = updateTexture,
      prioritize = prioritize,
      sendUpdatePacket = sendUpdatePacket
    )
  }

  override fun setBlock(
    localX: Int,
    localY: Int,
    block: Block?,
    updateTexture: Boolean,
    prioritize: Boolean,
    sendUpdatePacket: Boolean
  ): Block? {
    if (isDisposed) {
      Main.logger().warn("Changed block in disposed chunk ${stringifyChunkToWorld(this, localX, localY)}, block: $block")
      return null
    }
    if (block != null) {
      Preconditions.checkArgument(block.localX == localX)
      Preconditions.checkArgument(block.localY == localY)
      Preconditions.checkArgument(block.chunk === this)
    }
    val bothAirish: Boolean
    synchronized(this) {
      val currBlock = getRawBlock(localX, localY)
      if (currBlock === block) {
        return currBlock
      }
      // accounts for both being null also ofc
      bothAirish = areBothAirish(currBlock, block)
      if (bothAirish && currBlock != null) {
        // Ok to return here, an air block exists here and the new block is also air (or null)
        block?.dispose()
        return currBlock
      }
      if (currBlock != null) {
        currBlock.dispose()
        if (currBlock is TickingBlock) {
          tickingBlocks.removeAsync(currBlock)
        }
      }
      blocks[localX][localY] = block
      if (block is TickingBlock) {
        tickingBlocks.setAsync(block)
      }
      if (block != null && block.material.isLuminescent || currBlock != null && currBlock.material.isLuminescent) {
        updateBlockLights(localX, localY, true)
      }
      modified = true
      if (updateTexture) {
        dirty()
        this.prioritize = this.prioritize or prioritize // do not remove prioritization if chunk already is prioritized
      }
      if (!bothAirish) {
        if (currBlock != null) {
          chunkBody.removeBlock(currBlock)
        }
        if (block != null) {
          chunkBody.addBlock(block, null)
        }
        Main.inst().scheduler.executeAsync { chunkColumn.updateTopBlock(localX, getWorldY(localY)) }
      }
      dispatchEvent(BlockChangedEvent(currBlock, block))
    }
    val worldX = getWorldX(localX)
    val worldY = getWorldY(localY)
    if (sendUpdatePacket && isValid && !bothAirish) {
      if (Main.isServer()) {
        Main.inst()
          .scheduler
          .executeAsync {
            val packet = clientBoundBlockUpdate(worldX, worldY, block)
            broadcastToInView(packet, worldX, worldY, null)
          }
      } else if (Main.isServerClient()) {
        Main.inst()
          .scheduler
          .executeAsync {
            val client = ClientMain.inst().serverClient
            if (client != null) {
              val packet = client.serverBoundBlockUpdate(worldX, worldY, block)
              client.ctx.writeAndFlush(packet)
            }
          }
      }
    }
    if (updateTexture) {
      Main.inst().scheduler.executeAsync { world.updateBlocksAround(worldX, worldY) }
    }
    return block
  }

  override fun getWorldX(localX: Int): Int {
    return chunkToWorld(chunkX, localX)
  }

  override fun getWorldY(localY: Int): Int {
    return chunkToWorld(chunkY, localY)
  }

  @Synchronized
  override fun updateTexture(prioritize: Boolean) {
    dirty()
    modified = true
    this.prioritize = this.prioritize or prioritize
  }

  override val textureRegion: TextureRegion?
    get() {
      synchronized(fboLock) {
        if (isDirty) {
          updateIfDirty()
        }
        return fboRegion
      }
    }

  override fun hasTextureRegion(): Boolean {
    synchronized(fboLock) { return fboRegion != null }
  }

  override fun updateIfDirty() {
    if (isInvalid) {
      return
    }
    var wasPrioritize: Boolean
    synchronized(this) {
      if (!isDirty || initializing) {
        return
      }
      wasPrioritize = prioritize
      prioritize = false
      isDirty = false

      // test if all the blocks in this chunk has the material air
      allAir = true
      outer@ for (localX in 0 until Chunk.CHUNK_SIZE) {
        for (localY in 0 until Chunk.CHUNK_SIZE) {
          val b = blocks[localX][localY]
          if (b != null && b.material !== Material.AIR) {
            allAir = false
            break@outer
          }
        }
      }
    }

    // Render the world with the changes (but potentially without the light changes)
    val render = world.render
    if (render is ClientWorldRender) {
      render.chunkRenderer.queueRendering(this, wasPrioritize)
    }
  }

  private fun cancelCurrentBlockLightUpdate() {
    if (Settings.renderLight) {
      synchronized(blockLights) {
        // If we reached this point before the light is done recalculating then we must start again
        val currLU = lightUpdater
        if (currLU != null) {
          // Note that the previous thread will dispose itself (therefor it should not be
          // interrupted)
          currLU.cancel(false)
          lightUpdater = null
        }
      }
    }
  }

  override fun updateBlockLights(localX: Int, localY: Int, dispatchEvent: Boolean) {
    if (Settings.renderLight) {
      if (dispatchEvent) {
        dispatchEvent(ChunkLightUpdatedEvent(this, localX, localY))
      }
      synchronized(blockLights) {
        // If we reached this point before the light is done recalculating then we must start again
        cancelCurrentBlockLightUpdate()
        val updateId = currentUpdateId.incrementAndGet()
        lightUpdater = Main.inst().scheduler.executeAsync { updateBlockLights(updateId) }
      }
    }
  }

  /** Should only be used by [updateBlockLights]  */
  private fun updateBlockLights(updateId: Int) {
    val pool = ForkJoinPool.commonPool()
    synchronized(tasks) {
      outer@ for (localX in 0 until Chunk.CHUNK_SIZE) {
        for (localY in Chunk.CHUNK_SIZE - 1 downTo 0) {
          if (updateId != currentUpdateId.get()) {
            break@outer
          }
          val bl = blockLights[localX][localY]
          val task = pool.submit { bl.recalculateLighting(updateId) }
          task.fork()
          tasks.add(task)
        }
      }
      for (task in tasks) {
        if (updateId == currentUpdateId.get()) {
          try {
            task.join()
          } catch (ignore: CancellationException) {
          } catch (e: Exception) {
            e.printStackTrace()
          }
        } else {
          task.cancel(true)
        }
      }
      tasks.clear()
    }
    if (updateId == currentUpdateId.get()) {
      // TODO only re-render if any lights changed

      // Re-render the chunk with the new lighting
      val render = world.render
      if (render is ClientWorldRender) {
        render.chunkRenderer.queueRendering(this, true, true)
      }
    }
  }

  override fun view() {
    lastViewedTick = world.tick
  }

  override val frameBuffer: FrameBuffer?
    get() {
      if (isDisposed) {
        return null
      }
      synchronized(fboLock) {
        if (fbo != null) {
          return fbo
        }
        val fbo = FrameBuffer(Pixmap.Format.RGBA4444, Chunk.CHUNK_TEXTURE_SIZE, Chunk.CHUNK_TEXTURE_SIZE, true)
        fbo.colorBufferTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        val fboRegion = TextureRegion(fbo.colorBufferTexture)
        fboRegion.flip(false, true)
        this.fboRegion = fboRegion
        this.fbo = fbo
        return fbo
      }
    }

  /** Update all updatable blocks in this chunk  */
  override fun tick() {
    if (isInvalid) {
      return
    }
    tickingBlocks.tick(rare = false)
  }

  override fun tickRare() {
    if (isInvalid) {
      return
    }
    tickingBlocks.tick(rare = true)
  }

  override fun getBlockLight(localX: Int, localY: Int): BlockLight {
    return blockLights[localX][localY]
  }

  override fun getRawBlock(localX: Int, localY: Int): Block? {
    return blocks[localX][localY]
  }

  override val isAllAir: Boolean
    get() {
      if (isDirty) {
        updateIfDirty()
      }
      return allAir
    }

  override val isDisposed: Boolean get() = disposed

  override val isNotDisposed: Boolean
    get() = !isDisposed
  override val isValid: Boolean
    get() = !isDisposed && !initializing
  override val isInvalid: Boolean
    get() = isDisposed || initializing

  override fun setAllowUnload(allowUnload: Boolean) {
    if (isDisposed) {
      return // already unloaded
    }
    this.allowUnload = allowUnload
  }

  override val isAllowedToUnload: Boolean
    get() {
//      if (Settings.client) {
//              var player = ClientMain.inst().getPlayer();
//              if (player != null && equals(player.getChunk())) {
//                return false;
//              }
//      }
      return allowUnload
    }
  override val chunkColumn: ChunkColumn
    get() = world.getChunkColumn(chunkX)

  @get:Contract(pure = true)
  override val compactLocation: Long
    get() = compactLoc(chunkX, chunkY)
  override val worldX: Int
    /**
     * @return Location of this chunk in world coordinates
     */
    get() = chunkX.chunkToWorld()
  override val worldY: Int
    /**
     * This is the same as doing `CoordUtil.chunkToWorld(getLocation())`
     *
     * @return Location of this chunk in world coordinates
     */
    get() = chunkY.chunkToWorld()

  /**
   * @return If the chunk has been modified since creation
   */
  override fun shouldSave(): Boolean {
    return modified && isValid
  }

//    @Override
//    public Future<Array<Entity>> getEntities(Function1<Array<Entity>, Unit> callback) {
//      var future = new CompletableFuture<Array<Entity>>();
//      var array = new Array<Entity>(false, 16);
//      world.getWorldBody().queryAABB(getWorldX(), getWorldY(), getWorldX(Chunk.CHUNK_SIZE - 1),
//   getWorldY(Chunk.CHUNK_SIZE - 1), fixture -> {
//        Object userData = fixture.getUserData();
//        if (userData instanceof Entity entity) {
//          array.add(entity);
//        }
//        return true;
//      });
//      return future;
//    }

  override fun iterator(): Iterator<Block?> {
    return object : MutableIterator<Block?> {
      var x = 0
      var y = 0
      override fun hasNext(): Boolean {
        return y < Chunk.CHUNK_SIZE - 1 || x < Chunk.CHUNK_SIZE
      }

      override fun next(): Block? {
        if (x == Chunk.CHUNK_SIZE) {
          x = 0
          y++
        }
        if (y >= Chunk.CHUNK_SIZE) {
          throw NoSuchElementException()
        }
        return getRawBlock(x++, y)
      }

      override fun remove() {
        removeBlock(x, y)
      }
    }
  }

  /**
   * @param localX The local x ie a value between 0 and [CHUNK_SIZE]
   * @param localY The local y ie a value between 0 and [CHUNK_SIZE]
   * @return A block from the relative coordinates
   */
  override fun getBlock(localX: Int, localY: Int): Block {
    if (!isValid) {
      Main.logger()
        .warn(
          "Fetched block from invalid chunk " +
            stringifyChunkToWorld(this, localX, localY)
        )
    }
    Preconditions.checkArgument(
      isInsideChunk(localX, localY),
      "Given arguments are not inside this chunk, localX=$localX localY=$localY"
    )
    synchronized(this) {
      return blocks[localX][localY] ?: return setBlock(localX, localY, Material.AIR, updateTexture = false, sendUpdatePacket = false) ?: error("Failed to create air block!")
    }
  }

  @Synchronized
  override fun dispose() {
    if (isDisposed) {
      return
    }
    disposed = true
    allowUnload = false
    synchronized(fboLock) {
      fbo?.also {
        Main.inst().scheduler.executeSync { it.dispose() }
        fbo = null
      }
      fboRegion = null
    }
    chunkBody.dispose()
    tickingBlocks.clear()
    for (blockArr in blocks) {
      for (block in blockArr) {
        if (block != null && !block.isDisposed) {
          block.dispose()
        }
      }
    }
  }

  @Synchronized
  override fun dirty() {
    isDirty = true
  }

  /**
   * Internal loading of blocks have been completed, finish setting up the internal state but before
   * the chunks have been added to the world
   */
  @Synchronized
  fun finishLoading() {
    if (!initializing) {
      return
    }
    initializing = false
    dirty()
    tickingBlocks.setAll(blocks)
    chunkBody.update()
    // Register events
    registerListener(updateChunkLightEventListener)

    updateBlockLights()
  }

  override fun save(): ProtoWorld.Chunk {
    return save(true)
  }

  override fun saveBlocksOnly(): ProtoWorld.Chunk {
    return save(false)
  }

  private fun save(includeEntities: Boolean): ProtoWorld.Chunk {
    val builder = ProtoWorld.Chunk.newBuilder()
    builder.position = Vector2i.newBuilder().setX(chunkX).setY(chunkY).build()
    for (block in this) {
      builder.addBlocks(if (block != null) block.save() else AIR_BLOCK_BUILDER)
    }
//    if (includeEntities) {
    //      for (Entity entity : getEntities()) {
    //        if (entity instanceof Player) {
    //          continue;
    //        }
    //        builder.addEntities(entity.save());
    //      }
    // TODO do for ashley entities
//    }
    return builder.build()
  }

  override fun load(protoChunk: ProtoWorld.Chunk): Boolean {
    Preconditions.checkState(
      initializing,
      "Cannot load from proto chunk after chunk has been initialized"
    )
    val chunkPosition = protoChunk.position
    val posErrorMsg = (
      "Invalid chunk coordinates given. Expected (" +
        chunkX +
        ", " +
        chunkY +
        ") but got (" +
        chunkPosition.x +
        ", " +
        chunkPosition.y +
        ")"
      )
    Preconditions.checkArgument(chunkPosition.x == chunkX, posErrorMsg)
    Preconditions.checkArgument(chunkPosition.y == chunkY, posErrorMsg)
    Preconditions.checkArgument(
      protoChunk.blocksCount == Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE,
      "Invalid number of bytes. expected " + Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE + ", but got " +
        protoChunk.blocksCount
    )
    var index = 0
    val protoBlocks = protoChunk.blocksList
    synchronized(this) {
      for (localY in 0 until Chunk.CHUNK_SIZE) {
        for (localX in 0 until Chunk.CHUNK_SIZE) {
          check(blocks[localX][localY] == null) { "Double assemble" }
          val protoBlock = protoBlocks[index++]
          blocks[localX][localY] = Block.fromProto(world, this, localX, localY, protoBlock)
        }
      }
    }
    for (protoEntity in protoChunk.entitiesList) {
//            Entity.load(world, this, protoEntity);
    }
    return true
  }

  override fun hashCode(): Int {
    var result = world.hashCode()
    result = 31 * result + chunkX
    result = 31 * result + chunkY
    return result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is ChunkImpl) {
      return false
    }
    if (chunkX != other.chunkX) {
      return false
    }
    return if (chunkY != other.chunkY) {
      false
    } else {
      world == other.world
    }
  }

  override fun toString(): String {
    return "Chunk{world=$world, chunkX=$chunkX, chunkY=$chunkY, valid=$isValid}"
  }

  override fun compareTo(o: Chunk): Int {
    val compare = chunkX.compareTo(o.chunkX)
    return if (compare != 0) compare else chunkY.compareTo(o.chunkY)
  }

  companion object {
    private val AIR_BLOCK_BUILDER = Block.save(Material.AIR)

    private fun areBothAirish(blockA: Block?, blockB: Block?): Boolean {
      val a = blockA?.material ?: Material.AIR
      val b = blockB?.material ?: Material.AIR
      return a === Material.AIR && b === Material.AIR
    }
  }
}
