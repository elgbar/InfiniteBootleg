package no.elg.infiniteBootleg.world.chunks

import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.ChunkColumnFeatureFlag
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.chunks.Chunk.Companion.CHUNK_SIZE
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.TOP_MOST_FLAG
import no.elg.infiniteBootleg.world.world.World

/**
 * A column representing all chunks with the given [chunkX] coordinate.
 */
interface ChunkColumn {

  /**
   * The x-coordinate of the chunks represented by this column
   */
  val chunkX: ChunkCoord

  /**
   * The world this column belong
   */
  val world: World

  /**
   * @param localX offset into the chunk, must be in `0..`[CHUNK_SIZE]
   * @param features What kind of top block to return
   * @return the world y coordinate of the topmost block. Might return [FeatureFlag.FAILED_TO_FIND_TOP_BLOCK] if invalid flag
   *
   * @see FeatureFlag
   */
  fun topBlockHeight(localX: LocalCoord, features: ChunkColumnFeatureFlag = TOP_MOST_FLAG): WorldCoord

  /**
   * @param features What kind of top block to return
   * @return The block (including Air!) at the given local x, if `null` the chunk failed to load.
   *
   * @see FeatureFlag
   */
  fun topBlock(localX: LocalCoord, features: ChunkColumnFeatureFlag = TOP_MOST_FLAG): Block?

  /**
   * @param localX The x-coordinate of the block column to recalculate. Must be in `0..`[CHUNK_SIZE]
   * Recalculate the top block of a given local x
   */
  fun updateTopBlock(localX: LocalCoord)

  /**
   * @param localX The x-coordinate of the block column to recalculate. Must be in `0..`[CHUNK_SIZE]
   * @param worldYHint Hint what could be the next topmost block
   * Recalculate the top block of a given local x
   */
  fun updateTopBlock(localX: LocalCoord, worldYHint: WorldCoord)

  /**
   * @param features What kind of top block to return
   * @return Whether the chunk found at chunkY is STRICTLY above any of the chunks the topmost block are located in
   *
   * @see FeatureFlag
   */
  fun isChunkAboveTopBlock(chunkY: ChunkCoord, features: ChunkColumnFeatureFlag = TOP_MOST_FLAG): Boolean

  /**
   * @param features What kind of top block to return
   * @return If the block at the given position is above the top-most block
   *
   * @see FeatureFlag
   */
  fun isBlockAboveTopBlock(localX: LocalCoord, worldY: WorldCoord, features: ChunkColumnFeatureFlag = TOP_MOST_FLAG): Boolean

  /**
   * @return a copy of this column as a protobuf instance
   */
  fun toProtobuf(): ProtoWorld.ChunkColumn

  companion object {

    object FeatureFlag {
      /**
       * Indicate that the returned top block must block light
       */
      const val BLOCKS_LIGHT_FLAG: ChunkColumnFeatureFlag = 1

      /**
       * Indicate that the returned top block must be solid
       */
      const val SOLID_FLAG: ChunkColumnFeatureFlag = 2

      /**
       * The absolute top-most block, contains all flags
       */
      const val TOP_MOST_FLAG: ChunkColumnFeatureFlag = BLOCKS_LIGHT_FLAG or SOLID_FLAG

      val chunkColumnFeatureFlags = listOf(BLOCKS_LIGHT_FLAG, SOLID_FLAG)

      fun featureFlagToString(flag: ChunkColumnFeatureFlag): String =
        when (flag) {
          BLOCKS_LIGHT_FLAG -> "BLOCKS_LIGHT_FLAG"
          SOLID_FLAG -> "SOLID_FLAG"
          TOP_MOST_FLAG -> "TOP_MOST_FLAG"
          else -> "Unknown flag"
        }
    }
  }
}
