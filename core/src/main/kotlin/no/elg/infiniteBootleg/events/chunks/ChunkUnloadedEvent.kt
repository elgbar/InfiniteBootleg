package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.events.api.AsyncEvent
import no.elg.infiniteBootleg.events.api.ReasonedEvent
import no.elg.infiniteBootleg.events.api.ThreadType
import no.elg.infiniteBootleg.world.chunks.Chunk

/**
 * Fired when a chunk is about to be unloaded. It will still be valid while this event is dispatched
 */
data class ChunkUnloadedEvent(override val chunk: Chunk) : ChunkEvent, ReasonedEvent,
  AsyncEvent(ThreadType.TICKER, ThreadType.ASYNC, ThreadType.PHYSICS) {
  override val reason: String
    get() = "Chunk unloaded"
}
