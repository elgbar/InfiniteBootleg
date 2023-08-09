package no.elg.infiniteBootleg.world.generator.features

import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.world.chunks.Chunk

interface FeatureGenerator {
  val seed: Long

  /**
   * Generate some feature in the world
   */
  fun generateFeature(chunk: Chunk, worldX: WorldCoord, worldY: WorldCoord)
}
