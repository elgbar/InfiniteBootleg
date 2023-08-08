package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.events.api.AsyncEvent
import no.elg.infiniteBootleg.events.api.ThreadType
import no.elg.infiniteBootleg.world.chunks.Chunk

/**
 * Fired when a chunk has been fully loaded and can be used normally
 */
data class ChunkLoadedEvent(override val chunk: Chunk, val isNewlyGenerated: Boolean) : ChunkEvent, AsyncEvent(ThreadType.TICKER, ThreadType.ASYNC, ThreadType.PHYSICS)
