package no.elg.infiniteBootleg.core.events

import no.elg.infiniteBootleg.core.events.api.Event
import no.elg.infiniteBootleg.core.events.api.ReasonedEvent
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.world.chunks.Chunk

data class BlockLightChangedEvent(val chunk: Chunk, val localX: LocalCoord, val localY: LocalCoord) : ReasonedEvent, Event {
  override val reason: String
    get() = "Block light changed"
}
