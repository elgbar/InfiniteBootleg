package no.elg.infiniteBootleg.core.events.chunks

import no.elg.infiniteBootleg.core.events.api.AsyncEvent
import no.elg.infiniteBootleg.core.events.api.ReasonedEvent
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.world.chunks.Chunk

/**
 * Fired when a chunk has been fully loaded and can be used normally
 */
data class ChunkLoadedEvent(override val chunk: Chunk, val isNewlyGenerated: Boolean) :
  AsyncEvent(ThreadType.TICKER, ThreadType.ASYNC, ThreadType.PHYSICS),
  ChunkEvent,
  ReasonedEvent {
  override val reason: String
    get() = if (isNewlyGenerated) "Chunk was newly generated" else "Chunk was loaded from disk"
}
