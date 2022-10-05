package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.events.api.AsyncEvent
import no.elg.infiniteBootleg.events.api.ThreadType
import no.elg.infiniteBootleg.world.Chunk

/**
 * Fired when a chunk has been fully loaded and can be used normally
 */
data class ChunkLoadedEvent(override val chunk: Chunk) : ChunkEvent, AsyncEvent(ThreadType.TICKER, ThreadType.ASYNC)
