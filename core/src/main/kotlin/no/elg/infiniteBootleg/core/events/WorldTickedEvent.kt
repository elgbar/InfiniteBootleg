package no.elg.infiniteBootleg.core.events

import no.elg.infiniteBootleg.core.world.world.World

@Deprecated("Replace with a system")
data class WorldTickedEvent(override val world: World) : WorldEvent() {
  val tickId get() = world.tick
}
