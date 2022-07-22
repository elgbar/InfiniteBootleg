package no.elg.infiniteBootleg.world

/**
 * A column representing all chunks with a given chunk x coordinate.
 */
interface ChunkColumn {

  val chunkX: Int

  val world: World

  /**
   * @param localX offset into the chunk, must be in 0 .. [Chunk.CHUNK_SIZE]
   * @return the world y coordinate of the topmost block
   */
  fun topBlockHeight(localX: Int): Int
  fun topBlock(localX: Int): Block?
  fun updateTopBlock(localX: Int, worldYHint: Int)
  fun isChunkBelowTopBlock(chunkY: Int): Boolean
}
