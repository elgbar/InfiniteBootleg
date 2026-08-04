package no.elg.infiniteBootleg.core.events.chunks

import no.elg.infiniteBootleg.core.events.api.ReasonedEvent
import no.elg.infiniteBootleg.core.util.ChunkCompactLoc

data class ChunkTextureChangedEvent(override val chunkLoc: ChunkCompactLoc) :
  ChunkPositionEvent,
  ReasonedEvent {

  override val reason: String
    get() = "Chunk texture changed"
}
