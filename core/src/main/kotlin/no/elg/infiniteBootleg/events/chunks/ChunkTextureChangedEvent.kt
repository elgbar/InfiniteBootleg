package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.events.api.ReasonedEvent
import no.elg.infiniteBootleg.util.ChunkCompactLoc

data class ChunkTextureChangedEvent(override val chunkLoc: ChunkCompactLoc) : ChunkPositionEvent, ReasonedEvent {

  override val reason: String
    get() = "Chunk texture changed"
}
