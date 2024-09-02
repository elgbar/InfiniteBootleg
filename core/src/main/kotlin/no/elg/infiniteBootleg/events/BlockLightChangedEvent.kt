package no.elg.infiniteBootleg.events

import no.elg.infiniteBootleg.events.api.AsyncEvent
import no.elg.infiniteBootleg.events.api.ReasonedEvent
import no.elg.infiniteBootleg.events.api.ThreadType
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.world.chunks.Chunk

data class BlockLightChangedEvent(val chunk: Chunk, val localX: LocalCoord, val localY: LocalCoord) : ReasonedEvent, AsyncEvent(ThreadType.ASYNC) {
  override val reason: String
    get() = "Block light changed"
}
