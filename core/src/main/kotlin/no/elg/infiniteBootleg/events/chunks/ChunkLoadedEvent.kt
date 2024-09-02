package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.events.api.AsyncEvent
import no.elg.infiniteBootleg.events.api.ReasonedEvent
import no.elg.infiniteBootleg.events.api.ThreadType
import no.elg.infiniteBootleg.world.chunks.Chunk

/**
 * Fired when a chunk has been fully loaded and can be used normally
 */
data class ChunkLoadedEvent(override val chunk: Chunk, val isNewlyGenerated: Boolean) : ChunkEvent, ReasonedEvent,
  AsyncEvent(ThreadType.TICKER, ThreadType.ASYNC, ThreadType.PHYSICS) {
  override val reason: String
    get() = if (isNewlyGenerated) "Chunk was newly generated" else "Chunk was loaded from disk"
}
