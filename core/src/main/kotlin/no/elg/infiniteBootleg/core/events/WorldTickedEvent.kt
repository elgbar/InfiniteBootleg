package no.elg.infiniteBootleg.core.events

import no.elg.infiniteBootleg.core.world.world.World

data class WorldTickedEvent(override val world: World) : WorldEvent {
  val tickId get() = world.tick
}

data class WorldRareTickedEvent(override val world: World) : WorldEvent {
  val tickId get() = world.tick
}
