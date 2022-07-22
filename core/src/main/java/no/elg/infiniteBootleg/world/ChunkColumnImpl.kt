package no.elg.infiniteBootleg.world

import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.util.isNotAir
import no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE

class ChunkColumnImpl(override val world: World, override val chunkX: Int, initialTop: IntArray? = null) : ChunkColumn {

  private val top = IntArray(CHUNK_SIZE)
  private val syncLocks = Array(CHUNK_SIZE) { Any() }

  init {
    if (initialTop == null) {
      for (localX in 0 until CHUNK_SIZE) {
        val worldX = getWorldX(localX)
        top[localX] = world.chunkLoader.generator.getHeight(worldX)
      }
    } else {
      require(initialTop.size == CHUNK_SIZE) { "Chunk column was given initial top blocks with wrong size! Expected $CHUNK_SIZE, got ${initialTop.size}" }
      System.arraycopy(initialTop, 0, top, 0, CHUNK_SIZE)
    }
  }

  override fun topBlockHeight(localX: Int): Int {
    require(localX in 0 until CHUNK_SIZE) { "Local x is out of bounds. localX: $localX" }
    synchronized(syncLocks[localX]) {
      return top[localX]
    }
  }

  /**
   *
   * @return The block (including Air!) at the given local x, if `null` the chunk failed to load.
   */
  override fun topBlock(localX: Int): Block? {
    return getWorldBlock(localX, topBlockHeight(localX))
  }

  private fun getWorldX(localX: Int): Int {
    return CoordUtil.chunkToWorld(chunkX, localX)
  }

  private fun getWorldBlock(localX: Int, worldY: Int): Block? {
    return world.getBlock(getWorldX(localX), worldY)
  }

  private fun getLoadedChunk(worldY: Int): Chunk? {
    val chunkY = CoordUtil.worldToChunk(worldY)
    val compactLoc = CoordUtil.compactLoc(chunkX, chunkY)

    world.chunksReadLock.lock()
    try {
      return world.getChunks()[compactLoc]
    } finally {
      world.chunksReadLock.unlock()
    }
  }

  private fun getChunk(worldY: Int): Chunk? {
    val chunkX = CoordUtil.worldToChunk(chunkX)
    val chunkY = CoordUtil.worldToChunk(worldY)
    return world.getChunk(chunkX, chunkY)
  }

  private fun setTopBlock(localX: Int, worldY: Int) {
    synchronized(syncLocks[localX]) {
      top[localX] = worldY
    }
  }

  override fun updateTopBlock(localX: Int, worldYHint: Int) {
    synchronized(syncLocks[localX]) {
//      Main.logger().log("Updating localX $localX (chunk x $chunkX) with hint $worldYHint")
      val currTopBlock = topBlock(localX)
      if (currTopBlock == null) {
        // failed to get the current block
        Main.inst().scheduler.scheduleAsync(100) {
          updateTopBlock(localX, worldYHint)
        }
        return
      }
      val localY = CoordUtil.chunkOffset(worldYHint)
      val currTopHeight = topBlockHeight(localX)

      if (worldYHint > currTopHeight) {
        val hintChunk = getLoadedChunk(worldYHint)
        if (hintChunk != null && hintChunk.isValid) {
          // its loaded at least
          val hintBlock = hintChunk.getRawBlock(localX, localY)
          if (hintBlock.isNotAir()) {
            // Assume the hint was correct. This is now the top y!
            setTopBlock(localX, worldYHint)
            return
          }
        }
        // The hint was wrong!
        Main.logger().warn("ChunkCol", "WRONG gr top block hint! chunkx: $chunkX, localx: $localX, hint: $worldYHint valid chunk? ${hintChunk?.isValid ?: false}")
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
            if (nextBlock.isNotAir()) {
              setTopBlock(localX, CoordUtil.chunkToWorld(nextChunk.chunkY, nextLocalY))
              return true
            }
          }
        }
        return false
      }

      val currentTopChunkY = CoordUtil.worldToChunk(currTopHeight)

      for (nextChunkY in MAX_CHUNKS_TO_LOOK_UPWARDS downTo 0) {
        val nextChunk = getLoadedChunk(nextChunkY + currentTopChunkY)
        testChunk(nextChunk)
      }

      var nextChunkY = 0
      while (true) {
        val nextChunk = getChunk(currentTopChunkY - nextChunkY)
        if (testChunk(nextChunk)) {
          return
        }
        nextChunkY++
      }
    }
  }

  companion object {
    const val MAX_CHUNKS_TO_LOOK_UPWARDS = CHUNK_SIZE
  }
}
