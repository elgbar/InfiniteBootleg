package no.elg.infiniteBootleg.core.events

import it.unimi.dsi.fastutil.longs.LongSet
import no.elg.infiniteBootleg.core.events.api.AsyncEvent
import no.elg.infiniteBootleg.core.events.api.ReasonedEvent
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.util.WorldCompactLoc

data class LeafDecayCheckEvent(val compactBlockLoc: WorldCompactLoc, val seen: LongSet) :
  AsyncEvent(ThreadType.PHYSICS),
  ReasonedEvent {
  override val reason: String
    get() = "Checking if the leaf should decay"
}
