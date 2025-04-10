package no.elg.infiniteBootleg.core.events.chunks

import no.elg.infiniteBootleg.core.events.api.AsyncEvent
import no.elg.infiniteBootleg.core.events.api.ReasonedEvent
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.util.ChunkCompactLoc
import no.elg.infiniteBootleg.core.util.LocalCoord

/**
 * Indicates that a chunk's light is about to be updated
 */
data class ChunkLightChangedEvent(override val chunkLoc: ChunkCompactLoc, val localX: LocalCoord, val localY: LocalCoord) :
  AsyncEvent(ThreadType.ASYNC, ThreadType.PHYSICS),
  ChunkPositionEvent,
  ReasonedEvent {

  override val reason: String
    get() = "Chunk light changed"
}
