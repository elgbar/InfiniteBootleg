package no.elg.infiniteBootleg.world

import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.util.isNotAir
import no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE
import kotlin.contracts.ExperimentalContracts
import kotlin.math.max
import kotlin.math.min

@ExperimentalContracts
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

  /**
   * @return Whether the chunk found at chunkY is STRICTLY below any of the chunks the topmost block are located in
   */
  override fun isChunkBelowTopBlock(chunkY: Int): Boolean {
    val maxWorldY = CoordUtil.chunkToWorld(chunkY, CHUNK_SIZE - 1)
    for (localX in 0 until CHUNK_SIZE) {
      val topY = topBlockHeight(localX)
      if (topY > maxWorldY) {
        return true
      }
    }
    return false
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
    val chunkY = CoordUtil.worldToChunk(worldY)
    return world.getChunk(chunkX, chunkY)
  }

  private fun setTopBlock(localX: Int, worldY: Int) {
    val oldTop: Int

    // sanity check
    require(getWorldBlock(localX, worldY).isNotAir()) { "New top block is air!" }

    synchronized(syncLocks[localX]) {
      oldTop = topBlockHeight(localX)
      top[localX] = worldY
    }

    val min = min(oldTop, worldY)
    val max = max(oldTop, worldY)
    val fromClosedRange = IntProgression.fromClosedRange(min, max, CHUNK_SIZE)
    for (ycoord in fromClosedRange) {
      getLoadedChunk(ycoord)?.updateTexture(false)
    }
  }

  override fun updateTopBlock(localX: Int, worldYHint: Int) {
    synchronized(syncLocks[localX]) {
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

      @ExperimentalContracts
      fun testChunk(nextChunk: Chunk?): Boolean {
        if (nextChunk != null && nextChunk.isValid && !nextChunk.isAllAir) {
          for (nextLocalY in CHUNK_SIZE - 1 downTo 0) {
            val nextBlock = nextChunk.getRawBlock(localX, nextLocalY)
            if (nextBlock.isNotAir()) {
              setTopBlock(localX, nextBlock.worldY)
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