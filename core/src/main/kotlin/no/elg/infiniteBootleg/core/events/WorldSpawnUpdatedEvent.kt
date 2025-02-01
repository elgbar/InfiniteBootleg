package no.elg.infiniteBootleg.core.events

import no.elg.infiniteBootleg.core.util.WorldCompactLoc
import no.elg.infiniteBootleg.core.world.world.World

data class WorldSpawnUpdatedEvent(override val world: World, val oldSpawn: WorldCompactLoc, val newSpawn: WorldCompactLoc) : WorldEvent
