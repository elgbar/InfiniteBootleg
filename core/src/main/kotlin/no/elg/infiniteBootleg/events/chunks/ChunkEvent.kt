package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.util.ChunkCompactLoc
import no.elg.infiniteBootleg.world.chunks.Chunk

/**
 * An event that is related to a chunk, where the [chunk] instance is available
 */
interface ChunkEvent : ChunkPositionEvent {
  /**
   * The specific chunk that the event is related to
   */
  val chunk: Chunk

  override val chunkLoc: ChunkCompactLoc get() = chunk.compactLocation
}
