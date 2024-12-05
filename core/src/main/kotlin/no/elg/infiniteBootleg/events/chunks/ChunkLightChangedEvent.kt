package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.events.api.AsyncEvent
import no.elg.infiniteBootleg.events.api.ReasonedEvent
import no.elg.infiniteBootleg.events.api.ThreadType
import no.elg.infiniteBootleg.util.ChunkCompactLoc
import no.elg.infiniteBootleg.util.LocalCoord

/**
 * Indicates that a chunk's light is about to be updated
 */
data class ChunkLightChangedEvent(override val chunkLoc: ChunkCompactLoc, val localX: LocalCoord, val localY: LocalCoord) : ChunkPositionEvent, ReasonedEvent,
  AsyncEvent(ThreadType.ASYNC, ThreadType.PHYSICS) {

  override val reason: String
    get() = "Chunk light changed"
}
