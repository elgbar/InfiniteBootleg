package no.elg.infiniteBootleg.world.chunks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.physics.box2d.Body
import com.google.errorprone.annotations.concurrent.GuardedBy
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.Settings.handleChangingBlockInDeposedChunk
import no.elg.infiniteBootleg.events.BlockChangedEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.events.chunks.ChunkLightChangedEvent
import no.elg.infiniteBootleg.events.chunks.ChunkUnloadedEvent
import no.elg.infiniteBootleg.exceptions.checkChunkCorrupt
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.chunk
import no.elg.infiniteBootleg.protobuf.vector2i
import no.elg.infiniteBootleg.server.ServerClient.Companion.sendServerBoundPacket
import no.elg.infiniteBootleg.server.broadcastToInView
import no.elg.infiniteBootleg.server.clientBoundBlockUpdate
import no.elg.infiniteBootleg.server.serverBoundBlockUpdate
import no.elg.infiniteBootleg.util.ChunkCompactLoc
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.WorldCompactLocArray
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.chunkOffset
import no.elg.infiniteBootleg.util.chunkToWorld
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.util.dst2
import no.elg.infiniteBootleg.util.isInsideChunk
import no.elg.infiniteBootleg.util.isMarkerBlock
import no.elg.infiniteBootleg.util.launchOnAsync
import no.elg.infiniteBootleg.util.launchOnMain
import no.elg.infiniteBootleg.util.launchOnMultithreadedAsync
import no.elg.infiniteBootleg.util.singleLinePrinter
import no.elg.infiniteBootleg.util.stringifyChunkToWorld
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.blocks.Block.Companion.materialOrAir
import no.elg.infiniteBootleg.world.blocks.BlockLight
import no.elg.infiniteBootleg.world.box2d.ChunkBody
import no.elg.infiniteBootleg.world.chunks.Chunk.Companion.CHUNK_SIZE
import no.elg.infiniteBootleg.world.ecs.load
import no.elg.infiniteBootleg.world.ecs.save
import no.elg.infiniteBootleg.world.render.ClientWorldRender
import no.elg.infiniteBootleg.world.world.World
import org.jetbrains.annotations.Contract
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

class ChunkImpl(
  override val world: World,
  override val chunkX: ChunkCoord,
  override val chunkY: ChunkCoord
) : Chunk {

  val blocks: Array<Array<Block?>> = Array(CHUNK_SIZE) { arrayOfNulls(CHUNK_SIZE) }

  /**Single 1d array stored in a row major order*/
  private val blockLights = Array(CHUNK_SIZE * CHUNK_SIZE) { i -> BlockLight(this, i / CHUNK_SIZE, i % CHUNK_SIZE) }

  override val chunkBody: ChunkBody = ChunkBody(this)

  /**
   * if texture/allair needs to be updated
   *
   * Use [dirty] to mark chunk as dirty
   * */
  @Volatile
  override var isDirty = true

  /**
   * if this chunk should be prioritized to be updated
   */
  private var prioritize: Boolean = false

  /**
   * If the chunk has been modified since loaded
   */
  private var modified: Boolean = false

  /**
   * Whether this chunk is allowed to unload.
   * Sometimes we do not want to unload specific chunks, for example if there is a player in that chunk
   *  or it is the spawn chunk
   */
  private var allowUnload: Boolean = true

  /**
   * If the chunk is still being initialized, meaning not all blocks are in [blocks]
   */
  private var initializing: Boolean = true

  private var allAir: Boolean = false

  private var disposed: Boolean = false

  /**
   * @return The last tick this chunk's texture was pulled
   */
  override var lastViewedTick: Long = 0

  @GuardedBy("chunkBody")
  private var fbo: FrameBuffer? = null

  private val chunkListeners by lazy { ChunkListeners(this) }

  @Volatile
  var recalculateLightJob: Job? = null

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
      block = null,
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
      handleChangingBlockInDeposedChunk.handle {
        "Changed block in disposed chunk ${stringifyChunkToWorld(this, localX, localY)}, block: $block"
      }
      return null
    }
    if (block != null) {
      require(block.localX == localX) { "The local coordinate of the block does not match the given localX. Block localX: ${block.localX} != localX $localX" }
      require(block.localY == localY) { "The local coordinate of the block does not match the given localY. Block localY: ${block.localY} != localY $localY" }
      require(block.chunk === this) { "The chunk of the block is not this chunk. block chunk: ${block.chunk}, this: $this" }
    }
    val bothAirish: Boolean
    val currBlock = synchronized(blocks) {
      val currBlock = getRawBlock(localX, localY)
      if (currBlock == block) {
        block?.dispose()
        return currBlock
      }
      // accounts for both being null also ofc
      bothAirish = areBothAirish(currBlock, block)
      if (bothAirish && currBlock != null && (block == null || block::class == currBlock::class)) {
        // Ok to return here, an air block exists here and the new block is also air (or null)
        block?.dispose()
        return currBlock
      }
      blocks[localX][localY] = block
      currBlock
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
        chunkBody.addBlock(block)
      }
      launchOnAsync { chunkColumn.updateTopBlock(localX, getWorldY(localY)) }
    }

    if (!initializing && !bothAirish) {
      EventManager.dispatchEvent(BlockChangedEvent(currBlock, block))
    }
    if (block != null && block.material.emitsLight || currBlock != null && currBlock.material.emitsLight) {
      if (Settings.renderLight) {
        val originWorldX = getWorldX(localX)
        val originWorldY = getWorldY(localY)
        EventManager.dispatchEvent(ChunkLightChangedEvent(compactLocation, originWorldX.chunkOffset(), originWorldY.chunkOffset()))
      }
    }
    currBlock?.dispose()
    val worldX = getWorldX(localX)
    val worldY = getWorldY(localY)
    if (sendUpdatePacket && isValid && !bothAirish) {
      if (Main.isServer) {
        launchOnAsync {
          val packet = clientBoundBlockUpdate(worldX, worldY, block)
          broadcastToInView(packet, worldX, worldY)
        }
      } else if (Main.isServerClient && !block.isMarkerBlock()) {
        launchOnAsync {
          ClientMain.inst().serverClient.sendServerBoundPacket { serverBoundBlockUpdate(worldX, worldY, block) }
        }
      }
    }
    return block
  }

  override fun getWorldX(localX: LocalCoord): WorldCoord {
    return chunkX.chunkToWorld(localX)
  }

  override fun getWorldY(localY: LocalCoord): WorldCoord {
    return chunkY.chunkToWorld(localY)
  }

  @Synchronized
  override fun updateTexture(prioritize: Boolean) {
    dirty()
    modified = true
    this.prioritize = this.prioritize or prioritize
  }

  override val texture: Texture?
    get() {
      synchronized(chunkBody) {
        if (isDirty) {
          updateIfDirty()
        }
        return fbo?.colorBufferTexture
      }
    }

  override fun hasTexture(): Boolean = fbo != null

  /**
   * Force update of texture and recalculate internal variables This is usually called when the
   * dirty flag of the chunk is set and either [isAllAir] or [texture]
   * called.
   */
  private fun updateIfDirty() {
    if (isInvalid) {
      return
    }
    var wasPrioritize: Boolean
    synchronized(blocks) {
      if (!isDirty || initializing) {
        return
      }
      wasPrioritize = prioritize
      prioritize = false
      isDirty = false

      // test if all the blocks in this chunk has the material air
      allAir = true
      outer@ for (localX in 0 until CHUNK_SIZE) {
        for (localY in 0 until CHUNK_SIZE) {
          val b = blocks[localX][localY]
          if (b != null && b.material !== Material.AIR) {
            allAir = false
            break@outer
          }
        }
      }
    }

    // Render the world with the changes (but potentially without the light changes)
    queueForRendering(wasPrioritize)
  }

  /**
   * Queue this chunk to be rendered
   */
  override fun queueForRendering(prioritize: Boolean) {
    val render = world.render as? ClientWorldRender ?: return
    render.chunkRenderer.queueRendering(this, prioritize)
  }

  override fun updateAllBlockLights() {
    doUpdateLightMultipleSources(NOT_CHECKING_DISTANCE, checkDistance = false)
  }

  private fun isNoneWithinDistance(sources: WorldCompactLocArray, worldX: WorldCoord, worldY: WorldCoord): Boolean {
    for ((srcX: WorldCoord, srcY: WorldCoord) in sources) {
      val dstFromChange2blk = dst2(worldX, worldY, srcX, srcY)
      if (dstFromChange2blk <= World.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA_POW) {
        return false
      }
    }
    return true
  }

  internal fun doUpdateLightMultipleSources(sources: WorldCompactLocArray, checkDistance: Boolean) {
    if (isValid && world.isLoaded) {
      synchronized(this) { // TODO synchronize on something else
        if (!checkDistance) {
          // Safe to cancel when doing a full update
          // Note to self, DO NOT CANCEL when updating from sources,
          // as it might cancel updates to blocks that will not be updated in the next update
          recalculateLightJob?.cancel()
        }
        recalculateLightJob = launchOnMultithreadedAsync {
          doUpdateLightMultipleSources0(sources, checkDistance)
        }
      }
    }
  }

  private suspend fun doUpdateLightMultipleSources0(sources: WorldCompactLocArray, checkDistance: Boolean) {
    if (Settings.renderLight) {
      val anyRecalculated = AtomicBoolean(false)
      coroutineScope {
        for (localX in 0 until CHUNK_SIZE) {
          for (localY in CHUNK_SIZE - 1 downTo 0) {
            if (checkDistance && isNoneWithinDistance(sources, getWorldX(localX), getWorldY(localY))) {
              continue
            }
            launch {
              // TODO allow canceling of individual blocks
              val recalculated = getBlockLight(localX, localY).recalculateLighting()
              if (recalculated) {
                anyRecalculated.compareAndSet(false, true)
              }
            }
          }
        }
      }
      if (anyRecalculated.get()) {
        queueForRendering(prioritize = false)
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
      synchronized(chunkBody) {
        if (fbo != null) {
          return fbo
        }
        val fbo = FrameBuffer(Pixmap.Format.RGBA8888, Chunk.CHUNK_TEXTURE_SIZE, Chunk.CHUNK_TEXTURE_SIZE, false)
        fbo.colorBufferTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        this.fbo = fbo
        return fbo
      }
    }

  override fun getBlockLight(localX: LocalCoord, localY: LocalCoord): BlockLight {
    return blockLights[blockMapIndex(localX, localY)]
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

  override val isAllowedToUnload: Boolean get() = allowUnload
  override val chunkColumn: ChunkColumn
    get() = world.getChunkColumn(chunkX)

  @get:Contract(pure = true)
  override val compactLocation: ChunkCompactLoc = compactLoc(chunkX, chunkY)

  override val worldX: WorldCoord
    get() = chunkX.chunkToWorld()

  override val worldY: WorldCoord
    get() = chunkY.chunkToWorld()

  override fun shouldSave(): Boolean = modified

  override fun iterator(): Iterator<Block?> {
    return object : MutableIterator<Block?> {
      var x = 0
      var y = 0
      override fun hasNext(): Boolean {
        return y < CHUNK_SIZE - 1 || x < CHUNK_SIZE
      }

      override fun next(): Block? {
        if (x == CHUNK_SIZE) {
          x = 0
          y++
        }
        if (y >= CHUNK_SIZE) {
          throw NoSuchElementException()
        }
        return getRawBlock(x++, y)
      }

      override fun remove() {
        removeBlock(x, y)
      }
    }
  }

  override fun getBlock(localX: LocalCoord, localY: LocalCoord): Block {
    if (!isValid) {
      logger.warn { "Fetched block from invalid chunk ${stringifyChunkToWorld(this, localX, localY)}" }
    }
    require(isInsideChunk(localX, localY)) { "Given arguments are not inside this chunk, localX=$localX localY=$localY" }
    return getRawBlock(localX, localY) ?: setBlock(localX, localY, Material.AIR, updateTexture = false, sendUpdatePacket = false) ?: error("Failed to create air block!")
  }

  override fun getBlock(worldX: WorldCoord, worldY: WorldCoord, loadChunk: Boolean): Block? {
    if (compactLocation == compactLoc(worldX, worldY)) {
      return getBlock(worldX.chunkOffset(), worldY.chunkOffset())
    }
    return world.getBlock(worldX, worldY, loadChunk)
  }

  @Synchronized
  override fun dispose() {
    if (isDisposed) {
      return
    }
    EventManager.dispatchEvent(ChunkUnloadedEvent(this))
    disposed = true
    allowUnload = false
    chunkBody.dispose()
    chunkListeners.dispose()
    synchronized(chunkBody) {
      fbo?.also {
        launchOnMain { it.dispose() }
        fbo = null
      }
    }
    for (blockArr in blocks) {
      for (block in blockArr) {
        block?.dispose()
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
    chunkListeners.registerListeners()
    launchOnAsync {
      delay(200L)
      updateAllBlockLights()
    }
  }

  override fun queryEntities(callback: ((Set<Pair<Body, Entity>>) -> Unit)) =
    world.worldBody.queryEntities(
      chunkX.chunkToWorld(),
      chunkY.chunkToWorld(),
      chunkX.chunkToWorld(CHUNK_SIZE),
      chunkY.chunkToWorld(CHUNK_SIZE),
      callback
    )

  override fun save(): CompletableFuture<ProtoWorld.Chunk> {
    val future = CompletableFuture<ProtoWorld.Chunk>()
    chunk {
      position = vector2i {
        x = chunkX
        y = chunkY
      }
      blocks += this@ChunkImpl.map { it?.save() ?: AIR_BLOCK_PROTO }
      queryEntities { entities ->
        this.entities += entities.mapNotNull { (_, entity) ->
          if (world.playersEntities.contains(entity)) {
            // do not save players
            null
          } else {
            entity.save(toAuthoritative = true)
          }
        }
        val protoChunk = _build()
        if (Settings.debug && Settings.logPersistence) {
          logger.debug { singleLinePrinter.printToString(protoChunk) }
        }
        future.complete(protoChunk)
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
      blocks += this@ChunkImpl.map { it?.save() ?: AIR_BLOCK_PROTO }
    }

  override fun load(protoChunk: ProtoWorld.Chunk): Boolean {
    check(initializing) { "Cannot load from proto chunk after chunk has been initialized" }
    if (Settings.debug && Settings.logPersistence) {
      logger.debug { singleLinePrinter.printToString(protoChunk) }
    }
    val chunkPosition = protoChunk.position
    checkChunkCorrupt(protoChunk, chunkPosition.x == chunkX && chunkPosition.y == chunkY) {
      "Invalid chunk coordinates given. Expected ${stringifyCompactLoc(chunkX, chunkY)} but got ${
        stringifyCompactLoc(
          chunkPosition
        )
      }"
    }
    checkChunkCorrupt(protoChunk, protoChunk.blocksCount == CHUNK_SIZE * CHUNK_SIZE) {
      "Invalid number of blocks. Expected ${CHUNK_SIZE * CHUNK_SIZE}, but got ${protoChunk.blocksCount}"
    }

    var index = 0
    val protoBlocks = protoChunk.blocksList
    synchronized(blocks) {
      for (localY in 0 until CHUNK_SIZE) {
        for (localX in 0 until CHUNK_SIZE) {
          checkChunkCorrupt(protoChunk, blocks[localX][localY] == null) {
            "Double assemble of ${stringifyCompactLoc(localX, localY)} in chunk ${stringifyCompactLoc(chunkPosition)}"
          }
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
    return "Chunk{world=${world.name}, chunkX=$chunkX, chunkY=$chunkY, valid=$isValid}"
  }

  override fun compareTo(o: Chunk): Int {
    val compare = chunkX.compareTo(o.chunkX)
    return if (compare != 0) compare else chunkY.compareTo(o.chunkY)
  }

  companion object {
    val AIR_BLOCK_PROTO = Block.save(Material.AIR).build()
    val NOT_CHECKING_DISTANCE = WorldCompactLocArray(0)

    private fun blockMapIndex(localX: LocalCoord, localY: LocalCoord): Int = localX * CHUNK_SIZE + localY

    private fun areBothAirish(blockA: Block?, blockB: Block?): Boolean {
      return blockA.materialOrAir() === Material.AIR && blockB.materialOrAir() === Material.AIR
    }
  }
}
