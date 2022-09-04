package no.elg.infiniteBootleg.world

import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.util.isNotAir
import no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE
import no.elg.infiniteBootleg.world.ChunkColumn.Companion.BLOCKS_LIGHT_FLAG
import no.elg.infiniteBootleg.world.ChunkColumn.Companion.SOLID_FLAG
import kotlin.contracts.contract
import kotlin.math.max
import kotlin.math.min

class ChunkColumnImpl(override val world: World, override val chunkX: Int, initialTopSolid: IntArray? = null, initialTopLight: IntArray? = null) : ChunkColumn {

  private val topSolid = IntArray(CHUNK_SIZE)
  private val topLight = IntArray(CHUNK_SIZE)
  private val syncLocks = Array(CHUNK_SIZE) { Any() }

  init {
    if (initialTopSolid != null && initialTopLight != null) {
      require(initialTopSolid.size == CHUNK_SIZE) { "Chunk column was given initial top solid blocks with wrong size! Expected $CHUNK_SIZE, got ${initialTopSolid.size}" }
      require(initialTopLight.size == CHUNK_SIZE) { "Chunk column was given initial top light blocks with wrong size! Expected $CHUNK_SIZE, got ${initialTopLight.size}" }
      System.arraycopy(initialTopSolid, 0, topSolid, 0, CHUNK_SIZE)
      System.arraycopy(initialTopLight, 0, topLight, 0, CHUNK_SIZE)
    } else {
      for (localX in 0 until CHUNK_SIZE) {
        val worldX = getWorldX(localX)
        val height = world.chunkLoader.generator.getHeight(worldX)
        topSolid[localX] = height
        topLight[localX] = height
      }
    }
  }

  override fun topBlockHeight(localX: Int, features: Int): Int {
    require(localX in 0 until CHUNK_SIZE) { "Local x is out of bounds. localX: $localX" }
    synchronized(syncLocks[localX]) {
      val solid = if (features and SOLID_FLAG != 0) topSolid[localX] else Int.MIN_VALUE
      val light = if (features and BLOCKS_LIGHT_FLAG != 0) topLight[localX] else Int.MIN_VALUE
      val max = max(light, solid)
      require(max != Int.MIN_VALUE) { "Failed to find to block at local x $localX, with the given features $features in the chunk $chunkX column" }
      return max
    }
  }

  private fun topSkylight(localX: Int): Int {
    synchronized(syncLocks[localX]) {
      return topLight[localX]
    }
  }

  override fun topBlock(localX: Int, features: Int): Block? {
    return getWorldBlock(localX, topBlockHeight(localX, features))
  }

  override fun isChunkAboveTopBlock(chunkY: Int, features: Int): Boolean {
    val maxWorldY = CoordUtil.chunkToWorld(chunkY, CHUNK_SIZE - 1)
    for (localX in 0 until CHUNK_SIZE) {
      if (!isBlockAboveTopBlock(localX, maxWorldY, features)) {
        return false
      }
    }
    return true
  }

  override fun isBlockAboveTopBlock(localX: Int, worldY: Int, features: Int): Boolean {
    return topBlockHeight(localX, features) < worldY
  }

  private fun getWorldX(localX: Int): Int {
    return CoordUtil.chunkToWorld(chunkX, localX)
  }

  private fun getWorldBlock(localX: Int, worldY: Int): Block? {
    return world.getBlock(getWorldX(localX), worldY)
  }

  private fun getLoadedChunk(worldY: Int, chunkX: Int = this.chunkX): Chunk? {
    val compactLoc = CoordUtil.compactLoc(chunkX, CoordUtil.worldToChunk(worldY))
    return world.getLoadedChunk(compactLoc)
  }

  private fun getChunk(worldY: Int): Chunk? {
    val chunkY = CoordUtil.worldToChunk(worldY)
    return world.getChunk(chunkX, chunkY)
  }

  private fun setTopBlock(top: IntArray, localX: Int, worldY: Int) {
    val oldTop: Int

    // sanity check

    synchronized(syncLocks[localX]) {
      oldTop = topBlockHeight(localX)
      top[localX] = worldY
    }

    val oldChunk = CoordUtil.worldToChunk(oldTop)
    val newChunk = CoordUtil.worldToChunk(worldY)
    val min = min(oldChunk, newChunk) - 1
    val max = max(oldChunk, newChunk)
    val worldX = CoordUtil.chunkToWorld(chunkX, localX)
    for (chunkY in min..max) {
      val newWorldY = CoordUtil.chunkToWorld(chunkY)
      getLoadedChunk(newWorldY)?.updateBlockLights()

      // Update chunks to the sides, if the light reaches that far
      val leftChunkX = CoordUtil.worldToChunk((worldX - World.LIGHT_SOURCE_LOOK_BLOCKS).toInt())
      if (leftChunkX != chunkX) {
        getLoadedChunk(newWorldY, leftChunkX)?.updateBlockLights()
      }

      val rightChunkX = CoordUtil.worldToChunk((worldX + World.LIGHT_SOURCE_LOOK_BLOCKS).toInt())
      if (rightChunkX != chunkX) {
        getLoadedChunk(newWorldY, rightChunkX)?.updateBlockLights()
      }
    }
  }

  override fun updateTopBlock(localX: Int) {
    updateTopBlock(localX, topBlockHeight(localX))
  }

  override fun updateTopBlock(localX: Int, worldYHint: Int) {
    updateTopBlock(topSolid, localX, worldYHint) {
      it.isNotAir() && it.material.isSolid
    }

    updateTopBlock(topLight, localX, worldYHint) {
      it.isNotAir() && it.material.isBlocksLight
    }
  }

  // Extracting into an inlined function to allow usage of contract
  private inline fun isValidTopBlock(block: Block?, specialRule: (block: Block) -> Boolean): Boolean {
    contract { returns(true) implies (block != null) }
    return block.isNotAir() && specialRule(block)
  }

  private fun updateTopBlock(top: IntArray, localX: Int, worldYHint: Int, rule: (block: Block) -> Boolean) {
    synchronized(syncLocks[localX]) {
      val currTopHeight = top[localX]
      val currTopBlock = getWorldBlock(localX, top[localX])
      if (currTopBlock == null) {
        // failed to get the current block
        Main.inst().scheduler.scheduleAsync(100) {
          updateTopBlock(top, localX, worldYHint, rule)
        }
        return
      }

      val localY = CoordUtil.chunkOffset(worldYHint)

      if (worldYHint > currTopHeight) {
        val hintChunk = getLoadedChunk(worldYHint)
        if (hintChunk != null && hintChunk.isValid) {
          // its loaded at least
          val hintBlock = hintChunk.getRawBlock(localX, localY)
          if (isValidTopBlock(hintBlock, rule)) {
            // Assume the hint was correct. This is now the top y!
            setTopBlock(top, localX, worldYHint)
            return
          }
        }
        // The hint was wrong!
//        Main.logger().warn("ChunkCol", "WRONG gr top block hint! chunkx: $chunkX, localx: $localX, hint: $worldYHint valid chunk? ${hintChunk?.isValid ?: false}")
      }
      if (worldYHint < currTopHeight && currTopBlock.isNotAir()) {
        // Top block did not change
//        Main.logger().warn("ChunkCol", "WRONG lw top block hint! chunkx: $chunkX, localx:$localX, hint: $worldYHint")
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

      val currentTopChunkY = CoordUtil.worldToChunk(currTopHeight)

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
    builder.addAllTopSolidBlocks(topSolid.toList())
    builder.addAllTopTransparentBlocks(topLight.toList())
    return builder.build()
  }

  companion object {
    const val MAX_CHUNKS_TO_LOOK_UPWARDS = CHUNK_SIZE
    fun fromProtobuf(world: World, protoCC: ProtoWorld.ChunkColumn): ChunkColumn {
      return ChunkColumnImpl(world, protoCC.chunkX, protoCC.topSolidBlocksList.toIntArray(), protoCC.topTransparentBlocksList.toIntArray())
    }
  }
}
