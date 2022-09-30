package no.elg.infiniteBootleg.events

import no.elg.infiniteBootleg.events.api.AsyncEvent
import no.elg.infiniteBootleg.events.api.ThreadType
import no.elg.infiniteBootleg.world.Chunk

data class ChunkLoadedEvent(val chunk: Chunk) : AsyncEvent(ThreadType.TICKER, ThreadType.ASYNC)
