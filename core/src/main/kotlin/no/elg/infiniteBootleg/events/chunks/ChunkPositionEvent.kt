package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.events.api.Event
import no.elg.infiniteBootleg.util.ChunkCompactLoc

/**
 * The lightweight version of [ChunkEvent] that only contains the [ChunkCompactLoc] of the chunk
 */
interface ChunkPositionEvent : Event {

  /**
   * The position of the chunk
   */
  val chunkLoc: ChunkCompactLoc
}
