package no.elg.infiniteBootleg.core.world.generator.features

import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.world.chunks.Chunk

interface FeatureGenerator {
  val seed: Long

  /**
   * Generate some feature in the world
   */
  fun generateFeature(chunk: Chunk, worldX: WorldCoord, worldY: WorldCoord)
}
