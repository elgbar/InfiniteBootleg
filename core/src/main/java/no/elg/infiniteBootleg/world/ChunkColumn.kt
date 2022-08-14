package no.elg.infiniteBootleg.world

import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE

/**
 * A column representing all chunks with the given [chunkX] coordinate.
 */
interface ChunkColumn {

  /**
   * The x-coordinate of the chunks represented by this column
   */
  val chunkX: Int

  /**
   * The world this column belong
   */
  val world: World

  /**
   * @param localX offset into the chunk, must be in `0..`[CHUNK_SIZE]
   * @return the world y coordinate of the topmost block. Might return [Int.MIN_VALUE] if invalid flag
   */
  fun topBlockHeight(localX: Int, features: Int = TOP_MOST_FLAG): Int

  /**
   * @return The block (including Air!) at the given local x, if `null` the chunk failed to load.
   */
  fun topBlock(localX: Int, features: Int = TOP_MOST_FLAG): Block?

  /**
   * @param localX The x-coordinate of the block column to recalculate. Must be in `0..`[CHUNK_SIZE]
   * Recalculate the top block of a given local x
   */
  fun updateTopBlock(localX: Int)

  /**
   * @param localX The x-coordinate of the block column to recalculate. Must be in `0..`[CHUNK_SIZE]
   * @param worldYHint Hint what could be the next topmost block
   * Recalculate the top block of a given local x
   */
  fun updateTopBlock(localX: Int, worldYHint: Int)

  /**
   * @return Whether the chunk found at chunkY is STRICTLY above any of the chunks the topmost block are located in
   */
  fun isChunkAboveTopBlock(chunkY: Int, features: Int = TOP_MOST_FLAG): Boolean

  /**
   * @return If the block at the given position is above the top-most block
   */
  fun isBlockAboveTopBlock(localX: Int, worldY: Int, features: Int = TOP_MOST_FLAG): Boolean

  /**
   * @return a copy of this column as a protobuf instance
   */
  fun toProtobuf(): ProtoWorld.ChunkColumn

  companion object {
    /**
     * Indicate that the returned top block must block light
     */
    const val BLOCKS_LIGHT_FLAG = 1 shl 0

    /**
     * Indicate that the returned top block must be solid
     */
    const val SOLID_FLAG = 1 shl 2

    const val TOP_MOST_FLAG = BLOCKS_LIGHT_FLAG or SOLID_FLAG
  }
}
