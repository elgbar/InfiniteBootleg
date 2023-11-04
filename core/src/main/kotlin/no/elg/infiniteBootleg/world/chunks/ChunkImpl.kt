package no.elg.infiniteBootleg.world.chunks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.google.common.base.Preconditions
import com.google.protobuf.TextFormat
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.events.BlockChangedEvent
import no.elg.infiniteBootleg.events.api.EventManager.dispatchEvent
import no.elg.infiniteBootleg.events.api.EventManager.registerListener
import no.elg.infiniteBootleg.events.chunks.ChunkLightUpdatingEvent
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.chunk
import no.elg.infiniteBootleg.protobuf.vector2i
import no.elg.infiniteBootleg.server.broadcastToInView
import no.elg.infiniteBootleg.server.clientBoundBlockUpdate
import no.elg.infiniteBootleg.server.serverBoundBlockUpdate
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.chunkOffset
import no.elg.infiniteBootleg.util.chunkToWorld
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.isInsideChunk
import no.elg.infiniteBootleg.util.isMarkerBlock
import no.elg.infiniteBootleg.util.stringifyChunkToWorld
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.blocks.Block.Companion.materialOrAir
import no.elg.infiniteBootleg.world.blocks.BlockLight
import no.elg.infiniteBootleg.world.box2d.ChunkBody
import no.elg.infiniteBootleg.world.chunks.Chunk.Companion.CHUNK_CENTER
import no.elg.infiniteBootleg.world.ecs.load
import no.elg.infiniteBootleg.world.ecs.save
import no.elg.infiniteBootleg.world.render.ClientWorldRender
import no.elg.infiniteBootleg.world.world.World
import org.jetbrains.annotations.Contract
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinTask
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.concurrent.GuardedBy

class ChunkImpl(
  override val world: World,
  override val chunkX: ChunkCoord,
  override val chunkY: ChunkCoord
) : Chunk {

  val currentUpdateId = AtomicInteger()

  override val blocks: Array<Array<Block?>> = Array(Chunk.CHUNK_SIZE) { arrayOfNulls(Chunk.CHUNK_SIZE) }
  private val blockLights = Array(Chunk.CHUNK_SIZE) { x -> Array(Chunk.CHUNK_SIZE) { y -> BlockLight(this, x, y) } }

  override val chunkBody: ChunkBody = ChunkBody(this)

  @Volatile
  @GuardedBy("field")
  private var lightUpdater: ScheduledFuture<*>? = null
  private val tasks = ObjectSet<ForkJoinTask<*>>(Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE, 0.99f)

  /**
   * if texture/allair needs to be updated
   *
   * Use [dirty] to mark chunk as dirty
   * */
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

  @Contract("_, _, !null, _, _, _ -> !null; _, _, null, _, _, _ -> null")
  override fun setBlock(
    localX: LocalCoord,
    localY: LocalCoord,
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

  override fun removeBlock(
    localX: LocalCoord,
    localY: LocalCoord,
    updateTexture: Boolean,
    prioritize: Boolean,
    sendUpdatePacket: Boolean
  ) {
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
    localX: LocalCoord,
    localY: LocalCoord,
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
      if (bothAirish && currBlock != null && (block == null || block::class == currBlock::class)) {
        // Ok to return here, an air block exists here and the new block is also air (or null)
        block?.dispose()
        return currBlock
      }
      currBlock?.dispose()
      blocks[localX][localY] = block
      if (block != null && block.material.emitsLight || currBlock != null && currBlock.material.emitsLight) {
        blockLightUpdatedAt(localX, localY)
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
      if (Main.isServer) {
        Main.inst().scheduler.executeAsync {
          val packet = clientBoundBlockUpdate(worldX, worldY, block)
          broadcastToInView(packet, worldX, worldY)
        }
      } else if (Main.isServerClient && !block.isMarkerBlock()) {
        Main.inst().scheduler.executeAsync {
          val client = ClientMain.inst().serverClient
          if (client != null) {
            val packet = client.serverBoundBlockUpdate(worldX, worldY, block)
            client.ctx.writeAndFlush(packet)
          }
        }
      }
    }
    return block
  }

  override fun getWorldX(localX: LocalCoord): WorldCoord {
    return chunkToWorld(chunkX, localX)
  }

  override fun getWorldY(localY: LocalCoord): WorldCoord {
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

  /**
   * Force update of texture and recalculate internal variables This is usually called when the
   * dirty flag of the chunk is set and either [isAllAir] or [textureRegion]
   * called.
   */
  private fun updateIfDirty() {
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

  override fun updateAllBlockLights() {
    if (Settings.renderLight) {
      doUpdateLight(getWorldX(CHUNK_CENTER), getWorldY(CHUNK_CENTER), checkDistance = false, dispatchEvent = false)
    }
  }

  override fun blockLightUpdatedAt(localX: LocalCoord, localY: LocalCoord) {
    if (Settings.renderLight) {
      doUpdateLight(getWorldX(localX), getWorldY(localY), checkDistance = true, dispatchEvent = true)
    }
  }

  /**
   * Update the light of the chunk
   */
  internal fun doUpdateLight(originWorldX: WorldCoord, originWorldY: WorldCoord, checkDistance: Boolean, dispatchEvent: Boolean) {
    if (Settings.renderLight) {
      if (dispatchEvent) {
        dispatchEvent(ChunkLightUpdatingEvent(this, originWorldX.chunkOffset(), originWorldY.chunkOffset()))
      }
      synchronized(blockLights) {
        // If we reached this point before the light is done recalculating then we must start again
        cancelCurrentBlockLightUpdate()
        val updateId = currentUpdateId.incrementAndGet()
        lightUpdater = Main.inst().scheduler.executeAsync {
          synchronized(tasks) {
            outer@ for (localX in 0 until Chunk.CHUNK_SIZE) {
              for (localY in Chunk.CHUNK_SIZE - 1 downTo 0) {
                if (updateId != currentUpdateId.get()) {
                  break@outer
                }
                if (checkDistance) {
                  val dst2 = Vector2.dst2(originWorldX.toFloat(), originWorldY.toFloat(), getWorldX(localX).toFloat(), getWorldY(localY).toFloat())
                  if (dst2 > World.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA * World.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA) {
                    continue
                  }
                }
                blockLights[localX][localY].recalculateLighting(updateId)
              }
            }
          }
          if (updateId == currentUpdateId.get()) {
            // TODO only re-render if any lights changed

            // Re-render the chunk with the new lighting
            val render = world.render
            if (render is ClientWorldRender) {
              render.chunkRenderer.queueRendering(this, false)
            }
          }
        }
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
        val fbo = FrameBuffer(Pixmap.Format.RGBA8888, Chunk.CHUNK_TEXTURE_SIZE, Chunk.CHUNK_TEXTURE_SIZE, false)
        fbo.colorBufferTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        val fboRegion = TextureRegion(fbo.colorBufferTexture)
        fboRegion.flip(false, true)
        this.fboRegion = fboRegion
        this.fbo = fbo
        return fbo
      }
    }

  override fun getBlockLight(localX: LocalCoord, localY: LocalCoord): BlockLight {
    return blockLights[localX][localY]
  }

  override fun getRawBlock(localX: LocalCoord, localY: LocalCoord): Block? {
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
  override val worldX: WorldCoord
    /**
     * @return Location of this chunk in world coordinates
     */
    get() = chunkX.chunkToWorld()
  override val worldY: WorldCoord
    /**
     * This is the same as doing `CoordUtil.chunkToWorld(getLocation())`
     *
     * @return Location of this chunk in world coordinates
     */
    get() = chunkY.chunkToWorld()

  /**
   * @return If the chunk has been modified since creation
   */
  override fun shouldSave(): Boolean = modified

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
   * @return The block instance of the given coordinates, a new air block will be created if there is no existing block
   */
  override fun getBlock(localX: LocalCoord, localY: LocalCoord): Block {
    if (!isValid) {
      Main.logger().warn("Fetched block from invalid chunk ${stringifyChunkToWorld(this, localX, localY)}")
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
    chunkBody.dispose()
    synchronized(fboLock) {
      fbo?.also {
        Main.inst().scheduler.executeSync { it.dispose() }
        fbo = null
      }
      fboRegion = null
    }
    for (blockArr in blocks) {
      for (block in blockArr) {
        if (block != null && !block.isDisposed) {
          block.dispose()
        }
      }
    }
  }

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
    chunkBody.update()
    // Register events
    registerListener(updateChunkLightEventListener)
    registerListener(blockChangedEventListener)
    Main.inst().scheduler.executeAsync(::updateAllBlockLights)
  }

  fun queryEntities(callback: ((Iterable<Entity>) -> Boolean)) =
    world.worldBody.queryEntities(chunkX.chunkToWorld(), chunkY.chunkToWorld(), chunkToWorld(chunkX, Chunk.CHUNK_SIZE), chunkToWorld(chunkY, Chunk.CHUNK_SIZE), callback)

  override fun save(): CompletableFuture<ProtoWorld.Chunk> {
    val future = CompletableFuture<ProtoWorld.Chunk>()
    chunk {
      position = vector2i {
        x = chunkX
        y = chunkY
      }
      blocks += this@ChunkImpl.map { it?.save()?.build() ?: AIR_BLOCK_PROTO }
      queryEntities { entities ->
        this.entities += entities.mapNotNull { it.save(toAuthoritative = true) }
        future.complete(this._build())
      }
    }
    return future
  }

  override fun saveBlocksOnly() =
    chunk {
      position = vector2i {
        x = chunkX
        y = chunkY
      }
      blocks += this@ChunkImpl.map { it?.save()?.build() ?: AIR_BLOCK_PROTO }
    }

  override fun load(protoChunk: ProtoWorld.Chunk): Boolean {
    Preconditions.checkState(
      initializing,
      "Cannot load from proto chunk after chunk has been initialized"
    )

    Main.logger().debug("PB Chunk") { TextFormat.printer().shortDebugString(protoChunk) }
    val chunkPosition = protoChunk.position
    val posErrorMsg = "Invalid chunk coordinates given. Expected ($chunkX, $chunkY) but got (${chunkPosition.x}, ${chunkPosition.y})"
    Preconditions.checkArgument(chunkPosition.x == chunkX, posErrorMsg)
    Preconditions.checkArgument(chunkPosition.y == chunkY, posErrorMsg)
    Preconditions.checkArgument(
      protoChunk.blocksCount == Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE,
      "Invalid number of blocks. expected ${Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE}, but got ${protoChunk.blocksCount}"
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
      world.load(protoEntity, this)
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
    val AIR_BLOCK_PROTO_BUILDER = Block.save(Material.AIR)
    val AIR_BLOCK_PROTO = AIR_BLOCK_PROTO_BUILDER.build()

    private fun areBothAirish(blockA: Block?, blockB: Block?): Boolean {
      return blockA.materialOrAir() === Material.AIR && blockB.materialOrAir() === Material.AIR
    }
  }
}
