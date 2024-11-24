package no.elg.infiniteBootleg.events

import no.elg.infiniteBootleg.events.api.AsyncEvent
import no.elg.infiniteBootleg.events.api.ReasonedEvent
import no.elg.infiniteBootleg.events.api.ThreadType
import no.elg.infiniteBootleg.util.WorldCompactLoc

data class LeafDecayCheckEvent(val compactBlockLoc: WorldCompactLoc) : ReasonedEvent, AsyncEvent(ThreadType.PHYSICS) {
  override val reason: String
    get() = "Checking if the leaf should decay"
}
