package no.elg.infiniteBootleg.events

import no.elg.infiniteBootleg.util.WorldCompactLoc
import no.elg.infiniteBootleg.world.world.World

data class WorldSpawnUpdatedEvent(override val world: World, val oldSpawn: WorldCompactLoc, val newSpawn: WorldCompactLoc) : WorldEvent
