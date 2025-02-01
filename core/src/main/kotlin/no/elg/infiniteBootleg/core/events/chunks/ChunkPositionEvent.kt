package no.elg.infiniteBootleg.core.events.chunks

import no.elg.infiniteBootleg.core.events.api.Event
import no.elg.infiniteBootleg.core.util.ChunkCompactLoc

/**
 * The lightweight version of [ChunkEvent] that only contains the [no.elg.infiniteBootleg.core.util.ChunkCompactLoc] of the chunk
 */
interface ChunkPositionEvent : Event {

  /**
   * The position of the chunk
   */
  val chunkLoc: ChunkCompactLoc
}
