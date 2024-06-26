package no.elg.infiniteBootleg.events

import no.elg.infiniteBootleg.events.api.Event
import no.elg.infiniteBootleg.world.world.World

interface WorldEvent : Event {
  val world: World
}
