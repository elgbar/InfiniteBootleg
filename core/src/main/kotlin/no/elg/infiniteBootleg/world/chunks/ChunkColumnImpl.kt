package no.elg.infiniteBootleg.world.chunks

import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.ChunkColumnFeatureFlag
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.WorldCoordArray
import no.elg.infiniteBootleg.util.chunkOffset
import no.elg.infiniteBootleg.util.chunkToWorld
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.isNotAir
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.chunks.Chunk.Companion.CHUNK_SIZE
import no.elg.infiniteBootleg.world.chunks.Chunk.Companion.isValid
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.SOLID_FLAG
import no.elg.infiniteBootleg.world.world.World
import kotlin.contracts.contract
import kotlin.math.max
import kotlin.math.min

class ChunkColumnImpl(override val world: World, override val chunkX: ChunkCoord, initialTopSolid: WorldCoordArray? = null, initialTopLight: WorldCoordArray? = null) :
  ChunkColumn {

  private val topWorldYSolid = IntArray(CHUNK_SIZE)
  private val topWorldYLight = IntArray(CHUNK_SIZE)
  private val syncLocks = Array(CHUNK_SIZE) { Any() }

  init {
    if (initialTopSolid != null && initialTopLight != null) {
      require(initialTopSolid.size == CHUNK_SIZE) { "Chunk column was given initial top solid blocks with wrong size! Expected $CHUNK_SIZE, got ${initialTopSolid.size}" }
      require(initialTopLight.size == CHUNK_SIZE) { "Chunk column was given initial top light blocks with wrong size! Expected $CHUNK_SIZE, got ${initialTopLight.size}" }
      System.arraycopy(initialTopSolid, 0, topWorldYSolid, 0, CHUNK_SIZE)
      System.arraycopy(initialTopLight, 0, topWorldYLight, 0, CHUNK_SIZE)
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
    synchronized(syncLocks[localX]) {
      val solid = if (features and SOLID_FLAG != 0) topWorldYSolid[localX] else Int.MIN_VALUE
      val light = if (features and BLOCKS_LIGHT_FLAG != 0) topWorldYLight[localX] else Int.MIN_VALUE
      val max = max(light, solid)
      require(max != Int.MIN_VALUE) { "Failed to find to block at local x $localX, with the given features $features in the chunk $chunkX column" }
      return max
    }
  }

  private fun topSkylight(localX: LocalCoord): WorldCoord {
    synchronized(syncLocks[localX]) {
      return topWorldYLight[localX]
    }
  }

  override fun topBlock(localX: LocalCoord, features: ChunkColumnFeatureFlag): Block? {
    return getWorldBlock(localX, topBlockHeight(localX, features))
  }

  override fun isChunkAboveTopBlock(chunkY: ChunkCoord, features: ChunkColumnFeatureFlag): Boolean {
    val maxWorldY = chunkToWorld(chunkY, CHUNK_SIZE - 1)
    for (localX in 0 until CHUNK_SIZE) {
      if (!isBlockAboveTopBlock(localX, maxWorldY, features)) {
        return false
      }
    }
    return true
  }

  override fun isBlockAboveTopBlock(localX: LocalCoord, worldY: WorldCoord, features: ChunkColumnFeatureFlag): Boolean {
    return topBlockHeight(localX, features) < worldY
  }

  private fun getWorldX(localX: LocalCoord): WorldCoord {
    return chunkToWorld(chunkX, localX)
  }

  private fun getWorldBlock(localX: LocalCoord, worldY: WorldCoord): Block? {
    return world.getBlock(getWorldX(localX), worldY)
  }

  private fun getLoadedChunk(chunkY: ChunkCoord, chunkX: ChunkCoord = this.chunkX): Chunk? {
    val compactLoc = compactLoc(chunkX, chunkY)
    return world.getLoadedChunk(compactLoc)
  }

  private fun getLoadedChunkFromWorldY(worldY: WorldCoord, chunkX: ChunkCoord = this.chunkX): Chunk? = getLoadedChunk(worldY.worldToChunk(), chunkX)

  private fun getChunk(chunkY: ChunkCoord): Chunk? {
    return world.getChunk(chunkX, chunkY, true)
  }

  private fun setTopBlock(currentTops: WorldCoordArray, localX: LocalCoord, worldY: WorldCoord) {
    val oldTop: WorldCoord
    synchronized(syncLocks[localX]) {
      oldTop = currentTops[localX]
      currentTops[localX] = worldY
    }

    val oldChunk = oldTop.worldToChunk()
    val newChunk = worldY.worldToChunk()
    val min = min(oldChunk, newChunk) - 1
    val max = max(oldChunk, newChunk)
    val worldX = chunkToWorld(chunkX, localX)
    for (chunkY in min..max) {
      getLoadedChunk(chunkY)?.updateAllBlockLights()

      // Update chunks to the sides, if the light reaches that far
      val leftChunkX = (worldX - World.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA).toInt().worldToChunk()
      if (leftChunkX != chunkX) {
        getLoadedChunk(chunkY, leftChunkX)?.updateAllBlockLights()
      }

      val rightChunkX = (worldX + World.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA).toInt().worldToChunk()
      if (rightChunkX != chunkX) {
        getLoadedChunk(chunkY, rightChunkX)?.updateAllBlockLights()
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
    return block.isNotAir() && specialRule(block)
  }

  private fun updateTopBlock(top: WorldCoordArray, localX: LocalCoord, worldYHint: WorldCoord, rule: (block: Block) -> Boolean) {
    synchronized(syncLocks[localX]) {
      val currTopWorldY = top[localX]

      if (worldYHint > currTopWorldY) {
        // The hint is above the current top block. Check if it is valid
        val hintChunk = getLoadedChunkFromWorldY(worldYHint)
        if (hintChunk.isValid()) {
          // its loaded at least
          val localYHint = worldYHint.chunkOffset()
          val hintBlock = hintChunk.getRawBlock(localX, localYHint)
          if (isValidTopBlock(hintBlock, rule)) {
            // Assume the hint was correct. This is now the top y!
            setTopBlock(top, localX, worldYHint)
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

      fun testChunk(nextChunk: Chunk?): Boolean {
        if (nextChunk != null && nextChunk.isValid && !nextChunk.isAllAir) {
          for (nextLocalY in CHUNK_SIZE - 1 downTo 0) {
            val nextBlock = nextChunk.getRawBlock(localX, nextLocalY)
            if (isValidTopBlock(nextBlock, rule)) {
              setTopBlock(top, localX, nextBlock.worldY)
              return true
            }
          }
        }
        return false
      }

      val currentTopChunkY = currTopWorldY.worldToChunk()

      for (nextChunkY in MAX_CHUNKS_TO_LOOK_UPWARDS downTo 0) {
        val nextChunk = getLoadedChunk(nextChunkY + currentTopChunkY)
        if (testChunk(nextChunk)) {
          return
        }
      }

      for (nextChunkY in 0..Int.MAX_VALUE) {
        val nextChunk = getChunk(currentTopChunkY - nextChunkY)
        if (testChunk(nextChunk)) {
          return
        }
      }
    }
  }

  override fun toProtobuf(): ProtoWorld.ChunkColumn {
    val builder = ProtoWorld.ChunkColumn.newBuilder()
    builder.chunkX = chunkX
    builder.addAllTopSolidBlocks(topWorldYSolid.toList())
    builder.addAllTopTransparentBlocks(topWorldYLight.toList())
    return builder.build()
  }

  companion object {
    const val MAX_CHUNKS_TO_LOOK_UPWARDS = CHUNK_SIZE
    fun fromProtobuf(world: World, protoCC: ProtoWorld.ChunkColumn): ChunkColumn {
      return ChunkColumnImpl(world, protoCC.chunkX, protoCC.topSolidBlocksList.toIntArray(), protoCC.topTransparentBlocksList.toIntArray())
    }
  }
}
