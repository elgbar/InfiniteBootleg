package no.elg.infiniteBootleg.events

import no.elg.infiniteBootleg.events.api.Event
import no.elg.infiniteBootleg.world.world.World

/**
 * Event fired when the load is fully loaded and ready to be played on
 */
data class WorldLoadedEvent(val world: World) : Event
