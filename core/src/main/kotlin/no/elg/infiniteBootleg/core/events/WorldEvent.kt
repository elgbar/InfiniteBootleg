package no.elg.infiniteBootleg.core.events

import no.elg.infiniteBootleg.core.events.api.Event
import no.elg.infiniteBootleg.core.world.world.World

interface WorldEvent : Event {
  val world: World
}
