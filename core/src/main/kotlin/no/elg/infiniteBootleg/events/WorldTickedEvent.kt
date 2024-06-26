package no.elg.infiniteBootleg.events

import no.elg.infiniteBootleg.world.world.World

data class WorldTickedEvent(override val world: World) : WorldEvent
