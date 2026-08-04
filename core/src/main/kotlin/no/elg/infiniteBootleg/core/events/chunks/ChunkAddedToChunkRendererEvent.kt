package no.elg.infiniteBootleg.core.events.chunks

import no.elg.infiniteBootleg.core.events.api.ReasonedEvent
import no.elg.infiniteBootleg.core.util.ChunkCompactLoc
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc

data class ChunkAddedToChunkRendererEvent(override val chunkLoc: ChunkCompactLoc, val prioritized: Boolean) :
  ChunkPositionEvent,
  ReasonedEvent {

  override val reason: String
    get() = "Chunk ${stringifyCompactLoc(chunkLoc)} added to chunk renderer"
}
