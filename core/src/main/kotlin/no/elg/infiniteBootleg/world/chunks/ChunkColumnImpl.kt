package no.elg.infiniteBootleg.world.chunks

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.events.ChunkColumnUpdatedEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.chunkColumn
import no.elg.infiniteBootleg.util.ChunkColumnFeatureFlag
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.WorldCoordArray
import no.elg.infiniteBootleg.util.chunkOffset
import no.elg.infiniteBootleg.util.chunkToWorld
import no.elg.infiniteBootleg.util.isNotAir
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.chunks.Chunk.Companion.CHUNK_SIZE
import no.elg.infiniteBootleg.world.chunks.Chunk.Companion.valid
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.SOLID_FLAG
import no.elg.infiniteBootleg.world.world.World
import kotlin.contracts.contract
import kotlin.math.max

private val logger = KotlinLogging.logger {}

class ChunkColumnImpl(
  override val world: World,
  override val chunkX: ChunkCoord,
  initialTopSolid: WorldCoordArray? = null,
  initialTopLight: WorldCoordArray? = null
) : ChunkColumn {

  private val topWorldYSolid = IntArray(CHUNK_SIZE)
  private val topWorldYLight = IntArray(CHUNK_SIZE)
  private val syncLocks = Array(CHUNK_SIZE) { Any() }

  init {
    if (initialTopSolid != null && initialTopLight != null) {
      require(initialTopSolid.size == CHUNK_SIZE) { "Chunk column was given initial top solid blocks with wrong size! Expected $CHUNK_SIZE, got ${initialTopSolid.size}" }
      require(initialTopLight.size == CHUNK_SIZE) { "Chunk column was given initial top light blocks with wrong size! Expected $CHUNK_SIZE, got ${initialTopLight.size}" }
      initialTopSolid.copyInto(topWorldYSolid, endIndex = CHUNK_SIZE)
      initialTopSolid.copyInto(topWorldYLight, endIndex = CHUNK_SIZE)
    } else {
      for (localX in 0 until CHUNK_SIZE) {
        val worldX = getWorldX(localX)
        val height = world.chunkLoader.generator.getHeight(worldX)
        topWorldYSolid[localX] = height
        topWorldYLight[localX] = height
      }
    }
  }

  override fun topBlockHeight(localX: LocalCoord, features: ChunkColumnFeatureFlag): WorldCoord {
    require(localX in 0 until CHUNK_SIZE) { "Local x is out of bounds. localX: $localX" }
    val solid = if (features and SOLID_FLAG != 0) topWorldYSolid[localX] else Int.MIN_VALUE
    val light = if (features and BLOCKS_LIGHT_FLAG != 0) topWorldYLight[localX] else Int.MIN_VALUE
    val max = max(light, solid)
    require(max != Int.MIN_VALUE) { "Failed to find to block at local x $localX, with the given features $features in the chunk $chunkX column" }
    return max
  }

  override fun topBlock(localX: LocalCoord, features: ChunkColumnFeatureFlag): Block? = getWorldBlock(localX, topBlockHeight(localX, features))

  override fun isChunkAboveTopBlock(chunkY: ChunkCoord, features: ChunkColumnFeatureFlag): Boolean {
    val maxWorldY = chunkY.chunkToWorld(CHUNK_SIZE - 1)
    for (localX in 0 until CHUNK_SIZE) {
      if (!isBlockAboveTopBlock(localX, maxWorldY, features)) {
        return false
      }
    }
    return true
  }

  override fun isBlockAboveTopBlock(localX: LocalCoord, worldY: WorldCoord, features: ChunkColumnFeatureFlag): Boolean = topBlockHeight(localX, features) < worldY

  private fun getWorldX(localX: LocalCoord): WorldCoord = chunkX.chunkToWorld(localX)

  private fun getWorldBlock(localX: LocalCoord, worldY: WorldCoord): Block? = world.getBlock(getWorldX(localX), worldY)

  private fun getChunk(chunkY: ChunkCoord, chunkX: ChunkCoord = this.chunkX, load: Boolean = true): Chunk? = world.getChunk(chunkX, chunkY, load)

  private fun getLoadedChunkFromWorldY(worldY: WorldCoord, chunkX: ChunkCoord = this.chunkX): Chunk? = getChunk(worldY.worldToChunk(), chunkX, load = false)

  private fun setTopBlock(currentTops: WorldCoordArray, localX: LocalCoord, newWorldY: WorldCoord, expectedOldTopWorldY: WorldCoord) {
    val oldTop: WorldCoord
    synchronized(syncLocks[localX]) {
      oldTop = currentTops[localX]
      if (expectedOldTopWorldY != oldTop) {
        logger.debug { "Another thread updated the top block Y. Expected $expectedOldTopWorldY, currently it is $oldTop" }
        return
      }
      currentTops[localX] = newWorldY
    }

    if (expectedOldTopWorldY != newWorldY) {
      if (currentTops === topWorldYLight) {
        EventManager.dispatchEventAsync(ChunkColumnUpdatedEvent(chunkX, localX, newWorldY, oldTop, BLOCKS_LIGHT_FLAG))
      } else if (currentTops === topWorldYSolid) {
        EventManager.dispatchEventAsync(ChunkColumnUpdatedEvent(chunkX, localX, newWorldY, oldTop, SOLID_FLAG))
      }
    }
  }

  override fun updateTopBlock(localX: LocalCoord) {
    updateTopBlock(localX, topBlockHeight(localX))
  }

  override fun updateTopBlock(localX: LocalCoord, worldYHint: WorldCoord) {
    updateTopBlock(topWorldYSolid, localX, worldYHint) {
      it.material.isCollidable
    }

    updateTopBlock(topWorldYLight, localX, worldYHint) {
      it.material.blocksLight
    }
  }

  // Extracting into an inlined function to allow usage of contract
  private inline fun isValidTopBlock(block: Block?, specialRule: (block: Block) -> Boolean): Boolean {
    contract { returns(true) implies (block != null) }
    return block.isNotAir(markerIsAir = false) && specialRule(block)
  }

  private fun testChunk(
    nextChunk: Chunk,
    top: WorldCoordArray,
    localX: LocalCoord,
    expectedCurrentTopWorldY: WorldCoord,
    rule: (block: Block) -> Boolean
  ): Boolean {
    if (nextChunk.isValid && !nextChunk.isAllAir) {
      for (nextLocalY in CHUNK_SIZE - 1 downTo 0) {
        val nextBlock = nextChunk.getRawBlock(localX, nextLocalY)
        if (isValidTopBlock(nextBlock, rule)) {
          setTopBlock(top, localX, nextBlock.worldY, expectedCurrentTopWorldY)
          return true
        }
      }
    }
    return false
  }

  private fun updateTopBlock(top: WorldCoordArray, localX: LocalCoord, worldYHint: WorldCoord, rule: (block: Block) -> Boolean) {
    val currTopWorldY: WorldCoord = synchronized(syncLocks[localX]) {
      top[localX]
    }
    if (worldYHint > currTopWorldY) {
      // The hint is above the current top block. Check if it is valid
      val hintChunk = getLoadedChunkFromWorldY(worldYHint)
      if (hintChunk.valid()) {
        // it's loaded at least
        val localYHint = worldYHint.chunkOffset()
        val hintBlock = hintChunk.getRawBlock(localX, localYHint)
        if (isValidTopBlock(hintBlock, rule)) {
          // Assume the hint was correct. This is now the top y!
          setTopBlock(top, localX, worldYHint, currTopWorldY)
          return
        }
      }
    }

    val currTopLocalY = currTopWorldY.chunkOffset()
    val currTopChunk = getLoadedChunkFromWorldY(currTopWorldY)
    val currTopBlock: Block? = currTopChunk?.getRawBlock(localX, currTopLocalY)
    if (worldYHint < currTopWorldY && (currTopChunk == null || currTopBlock.isNotAir())) {
      // World y hint is below the current top block.
      // But the current top block is not air or the chunk is not loaded, so the top block (should) not have changed
      return
    }

    val currentTopChunkY = currTopWorldY.worldToChunk()

    for (nextChunkY in MAX_CHUNKS_TO_LOOK downTo 0) {
      // Find the top-most loaded chunk in this column, do not load chunks for improved performance
      val nextChunk = getChunk(nextChunkY + currentTopChunkY, load = false) ?: continue
      if (testChunk(nextChunk, top, localX, currTopWorldY, rule)) {
        return
      }
    }

    for (nextChunkY in 0..MAX_CHUNKS_TO_LOOK) {
      // If the chunk is not loaded we can safely assume no other chunks above are loaded
      val nextChunk = getChunk(currentTopChunkY - nextChunkY, load = false) ?: break
      if (testChunk(nextChunk, top, localX, currTopWorldY, rule)) {
        return
      }
    }
  }

  override fun toProtobuf(): ProtoWorld.ChunkColumn =
    chunkColumn {
      chunkX = chunkX
      topSolidBlocks += topWorldYSolid.asIterable()
      topTransparentBlocks += topWorldYLight.asIterable()
    }

  companion object {
    const val MAX_CHUNKS_TO_LOOK = CHUNK_SIZE
    fun fromProtobuf(world: World, protoCC: ProtoWorld.ChunkColumn): ChunkColumn =
      ChunkColumnImpl(world, protoCC.chunkX, protoCC.topSolidBlocksList.toIntArray(), protoCC.topTransparentBlocksList.toIntArray())
  }
}
