package no.elg.infiniteBootleg.core.world.chunks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.box2d.structs.b2BodyId
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.events.BlockChangedEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.chunks.ChunkLightChangedEvent
import no.elg.infiniteBootleg.core.events.chunks.ChunkUnloadedEvent
import no.elg.infiniteBootleg.core.exceptions.checkChunkCorrupt
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.net.clientBoundBlockUpdate
import no.elg.infiniteBootleg.core.net.serverBoundBlockUpdate
import no.elg.infiniteBootleg.core.util.ChunkCompactLoc
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.WorldCompactLocArray
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.chunkOffset
import no.elg.infiniteBootleg.core.util.chunkToWorld
import no.elg.infiniteBootleg.core.util.compactInt
import no.elg.infiniteBootleg.core.util.component1
import no.elg.infiniteBootleg.core.util.component2
import no.elg.infiniteBootleg.core.util.dst2
import no.elg.infiniteBootleg.core.util.isAir
import no.elg.infiniteBootleg.core.util.isInsideChunk
import no.elg.infiniteBootleg.core.util.isMarkerBlock
import no.elg.infiniteBootleg.core.util.launchOnAsyncSuspendable
import no.elg.infiniteBootleg.core.util.launchOnMultithreadedAsyncSuspendable
import no.elg.infiniteBootleg.core.util.singleLinePrinter
import no.elg.infiniteBootleg.core.util.stringifyChunkToWorld
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.BlockLight
import no.elg.infiniteBootleg.core.world.box2d.ChunkBody
import no.elg.infiniteBootleg.core.world.ecs.load
import no.elg.infiniteBootleg.core.world.ecs.save
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.chunk
import no.elg.infiniteBootleg.protobuf.vector2i
import org.jetbrains.annotations.Contract
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

open class ChunkImpl(final override val world: World, final override val chunkX: ChunkCoord, final override val chunkY: ChunkCoord) : Chunk {

  val blocks: Array<Array<Block?>> = Array(Chunk.CHUNK_SIZE) { arrayOfNulls(Chunk.CHUNK_SIZE) }

  /**Single 1d array stored in a row major order*/
  private val blockLights = Array(Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE) { i ->
    BlockLight(
      this,
      i / Chunk.CHUNK_SIZE,
      i % Chunk.CHUNK_SIZE
    )
  }

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
  protected var prioritize: Boolean = false

  /**
   * If the chunk has been modified since loaded
   */
  protected var modified: Boolean = false

  override var allowedToUnload: Boolean = true
    set(value) {
      field = if (isDisposed) {
        // already unloaded, then it must have been allowed to be unloaded
        true
      } else {
        value
      }
    }

  /**
   * If the chunk is still being initialized, meaning not all blocks are in [blocks]
   */
  protected var initializing: Boolean = true

  override var isAllAir: Boolean = false
    get() {
      updateIfDirty()
      return field
    }

  private var disposed: Boolean = false

  private val chunkListeners by lazy { ChunkListeners(this) }

  @Volatile
  var recalculateLightJob: Job? = null

  /**
   * Force update of texture and recalculate internal variables This is usually called when the
   * dirty flag of the chunk is set and either [isAllAir] called.
   *
   * @return If this chunk was prioritized
   */
  override fun updateIfDirty(): Boolean {
    if (isInvalid || !isDirty) {
      return false
    }
    val wasPrioritize: Boolean
    synchronized(blocks) {
      wasPrioritize = prioritize
      if (!isDirty || initializing) {
        return wasPrioritize
      }
      prioritize = false
      isDirty = false

      // test if all the blocks in this chunk has the material air
      isAllAir = true
      outer@ for (localX in 0 until Chunk.CHUNK_SIZE) {
        for (localY in 0 until Chunk.CHUNK_SIZE) {
          val b = blocks[localX][localY]
          if (b != null && b.material !== Material.Air) {
            isAllAir = false
            break@outer
          }
        }
      }
    }
    return wasPrioritize
  }

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
      Settings.handleChangingBlockInDeposedChunk.handle {
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
      dirty(prioritize)
    }
    if (!bothAirish) {
      // Note chunkBody must be called after the body is inserted into the chunk
      if (currBlock != null) {
        chunkBody.removeBlock(currBlock)
      }
      if (block != null) {
        chunkBody.addBlock(block)
      }
      launchOnAsyncSuspendable { chunkColumn.updateTopBlock(localX, chunkY.chunkToWorld(localY)) }
    }

    if (!initializing && !bothAirish) {
      EventManager.dispatchEventAsync(BlockChangedEvent(currBlock, block))
    }
    if (block != null && block.material.emitsLight || currBlock != null && currBlock.material.emitsLight) {
      if (Settings.renderLight) {
        EventManager.dispatchEventAsync(ChunkLightChangedEvent(compactLocation, localX, localY))
      }
    }
    currBlock?.dispose()
    if (Main.isMultiplayer && sendUpdatePacket && isValid && !bothAirish && !block.isMarkerBlock()) {
      val worldX = chunkX.chunkToWorld(localX)
      val worldY = chunkY.chunkToWorld(localY)
      Main.inst().packetSender.sendDuplexPacketInView(
        ifIsServer = { clientBoundBlockUpdate(worldX, worldY, block) to compactLocation },
        ifIsClient = { serverBoundBlockUpdate(worldX, worldY, block) }
      )
    }
    return block
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
    if (Settings.renderLight && isValid && world.isLoaded) {
      synchronized(this) {
        // TODO synchronize on something else
        if (!checkDistance) {
          // Safe to cancel when doing a full update
          // Note to self, DO NOT CANCEL when updating from sources,
          // as it might cancel updates to blocks that will not be updated in the next update
          recalculateLightJob?.cancel()
        }
        recalculateLightJob = launchOnMultithreadedAsyncSuspendable {
          doUpdateLightMultipleSources0(sources, checkDistance)
        }
      }
    }
  }

  /**
   * @return if any block was recalculated
   */
  protected open suspend fun doUpdateLightMultipleSources0(sources: WorldCompactLocArray, checkDistance: Boolean): Boolean {
    if (Settings.renderLight) {
      val anyRecalculated = AtomicBoolean(false)
      coroutineScope {
        for (localX in 0 until Chunk.CHUNK_SIZE) {
          for (localY in Chunk.CHUNK_SIZE - 1 downTo 0) {
            if (checkDistance &&
              isNoneWithinDistance(
                sources,
                this@ChunkImpl.chunkX.chunkToWorld(localX),
                this@ChunkImpl.chunkY.chunkToWorld(localY)
              )
            ) {
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
      return anyRecalculated.get()
    }
    return false
  }

  override fun getBlockLight(localX: LocalCoord, localY: LocalCoord): BlockLight = blockLights[blockMapIndex(localX, localY)]

  override fun getRawBlock(localX: LocalCoord, localY: LocalCoord): Block? = blocks[localX][localY]

  override val isDisposed: Boolean get() = disposed

  override val isNotDisposed: Boolean
    get() = !disposed
  override val isValid: Boolean
    get() = !disposed && !initializing
  override val isInvalid: Boolean
    get() = disposed || initializing

  override val chunkColumn: ChunkColumn
    get() = world.getChunkColumn(chunkX)

  @get:Contract(pure = true)
  override val compactLocation: ChunkCompactLoc = compactInt(chunkX, chunkY)

  override val worldX: WorldCoord
    get() = chunkX.chunkToWorld()

  override val worldY: WorldCoord
    get() = chunkY.chunkToWorld()

  override fun shouldSave(): Boolean = modified

  override fun iterator(): Iterator<Block?> {
    return object : MutableIterator<Block?> {
      var x = 0
      var y = 0
      override fun hasNext(): Boolean = y < Chunk.CHUNK_SIZE - 1 || x < Chunk.CHUNK_SIZE

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

  override fun getBlock(localX: LocalCoord, localY: LocalCoord): Block {
    if (!isValid) {
      logger.warn { "Fetched block from invalid chunk ${stringifyChunkToWorld(this, localX, localY)}" }
    }
    require(
      isInsideChunk(
        localX,
        localY
      )
    ) { "Given arguments are not inside this chunk, localX=$localX localY=$localY" }
    return getRawBlock(localX, localY) ?: setBlock(
      localX,
      localY,
      Material.Air,
      updateTexture = false,
      sendUpdatePacket = false
    ) ?: error("Failed to create air block!")
  }

  override fun getBlock(worldX: WorldCoord, worldY: WorldCoord, loadChunk: Boolean): Block? {
    if (compactLocation == compactInt(worldX, worldY)) {
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
    chunkBody.dispose()
    chunkListeners.dispose()
    for (blockArr in blocks) {
      for (block in blockArr) {
        block?.dispose()
      }
    }
  }

  override fun dirty(prioritize: Boolean) {
    isDirty = true
    this.prioritize = this.prioritize or prioritize
  }

  override fun updateAllBlockLights() {
    doUpdateLightMultipleSources(NOT_CHECKING_DISTANCE, checkDistance = false)
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
    launchOnAsyncSuspendable {
      delay(200L)
      updateAllBlockLights()
    }
  }

  override fun queryEachEntities(callback: ((b2BodyId, Entity) -> Unit)) {
    world.worldBody.queryEntities(
      chunkX.chunkToWorld(),
      chunkY.chunkToWorld(),
      chunkX.chunkToWorld(Chunk.CHUNK_SIZE),
      chunkY.chunkToWorld(Chunk.CHUNK_SIZE),
      EMPTY_AABB_QUERY_AFTER_ALL_CALLBACK,
      callback
    )
  }

  override fun queryAllEntities(afterAllCallback: (Set<Entity>) -> Unit) {
    world.worldBody.queryEntities(
      chunkX.chunkToWorld(),
      chunkY.chunkToWorld(),
      chunkX.chunkToWorld(Chunk.CHUNK_SIZE),
      chunkY.chunkToWorld(Chunk.CHUNK_SIZE),
      afterAllCallback,
      EMPTY_AABB_QUERY_FOR_EACH_CALLBACK
    )
  }

  override fun save(): CompletableFuture<ProtoWorld.Chunk> {
    val future = CompletableFuture<ProtoWorld.Chunk>()
    val protoChunkWithoutEntities = saveBlocksOnly()
    queryAllEntities { entities ->
      val protoWorldEntities = entities.mapNotNull { entity ->
        if (entity in world.playersEntities) {
          // do not save players
          null
        } else {
          entity.save(toAuthoritative = true)
        }
      }
      val protoChunk = protoChunkWithoutEntities.toBuilder().addAllEntities(protoWorldEntities).build()
      if (Settings.debug && Settings.logPersistence) {
        logger.debug { singleLinePrinter.printToString(protoChunk) }
      }
      future.complete(protoChunk)
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
    checkChunkCorrupt(protoChunk, protoChunk.blocksCount == Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE) {
      "Invalid number of blocks. Expected ${Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE}, but got ${protoChunk.blocksCount}"
    }

    var index = 0
    val protoBlocks = protoChunk.blocksList
    synchronized(blocks) {
      for (localY in 0 until Chunk.CHUNK_SIZE) {
        for (localX in 0 until Chunk.CHUNK_SIZE) {
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

  override fun toString(): String = "Chunk{world=${world.name}, chunkX=$chunkX, chunkY=$chunkY, valid=$isValid}"

  override fun compareTo(other: Chunk): Int {
    val compare = chunkX.compareTo(other.chunkX)
    return if (compare != 0) compare else chunkY.compareTo(other.chunkY)
  }

  companion object {
    val AIR_BLOCK_PROTO: ProtoWorld.Block = Block.save(Material.Air).build()
    val NOT_CHECKING_DISTANCE = LongArray(0)

    private val EMPTY_AABB_QUERY_AFTER_ALL_CALLBACK: (Set<Entity>) -> Unit = { }
    private val EMPTY_AABB_QUERY_FOR_EACH_CALLBACK: (b2BodyId, Entity) -> Unit = { _, _ -> }

    @Suppress("NOTHING_TO_INLINE")
    @Contract(pure = true)
    private inline fun blockMapIndex(localX: LocalCoord, localY: LocalCoord): Int = localX * Chunk.CHUNK_SIZE + localY

    @Contract(pure = true)
    private fun areBothAirish(blockA: Block?, blockB: Block?): Boolean = blockA.isAir(markerIsAir = false) && blockB.isAir(markerIsAir = false)
  }
}
