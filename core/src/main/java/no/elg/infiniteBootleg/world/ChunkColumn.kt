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

  /**
   * @return The block (including Air!) at the given local x, if `null` the chunk failed to load.
   */
  fun topBlock(localX: Int): Block?

  fun updateTopBlock(localX: Int, worldYHint: Int)

  /**
   * @return Whether the chunk found at chunkY is STRICTLY above any of the chunks the topmost block are located in
   */
  fun isChunkAboveTopBlock(chunkY: Int): Boolean
  fun isBlockSkylight(localX: Int, worldY: Int): Boolean
}
