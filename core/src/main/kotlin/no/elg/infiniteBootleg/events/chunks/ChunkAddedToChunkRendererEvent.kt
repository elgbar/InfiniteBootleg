package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.events.api.ReasonedEvent
import no.elg.infiniteBootleg.util.ChunkCompactLoc
import no.elg.infiniteBootleg.util.stringifyCompactLoc

data class ChunkAddedToChunkRendererEvent(override val chunkLoc: ChunkCompactLoc, val prioritized: Boolean) : ChunkPositionEvent, ReasonedEvent {

  override val reason: String
    get() = "Chunk ${stringifyCompactLoc(chunkLoc)} added to chunk renderer"
}
