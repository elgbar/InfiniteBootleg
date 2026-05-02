package no.elg.infiniteBootleg.core.events

import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.events.api.ThreadedEvent
import no.elg.infiniteBootleg.core.world.world.World

abstract class WorldEvent : ThreadedEvent(ThreadType.PHYSICS) {
  abstract val world: World
}
