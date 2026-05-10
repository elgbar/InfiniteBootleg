package no.elg.infiniteBootleg.core.world.chunks

import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.WorldCompactLocArray
import no.elg.infiniteBootleg.core.world.blocks.BlockLight

interface LitChunk {

  /** Update the light of the chunk  */
  fun updateAllBlockLights()

  fun getBlockLight(localX: LocalCoord, localY: LocalCoord): BlockLight

  /** Add a single world-coordinate light source to the chunk's pending-flush queue. */
  fun queueLightSource(compactWorldLoc: Long)

  /** Add many world-coordinate light sources to the chunk's pending-flush queue. */
  fun queueLightSources(compactWorldLocs: WorldCompactLocArray)

  /**
   * Drain the pending-flush queue and recalculate lighting for the affected blocks.
   * Cheap fast-path when the queue is empty.
   */
  fun flushPendingLightUpdates()
}
