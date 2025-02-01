package no.elg.infiniteBootleg.core.events

import no.elg.infiniteBootleg.core.world.world.World

/**
 * Event fired when the load is fully loaded and ready to be played on
 */
data class WorldLoadedEvent(override val world: World) : WorldEvent
