package no.elg.infiniteBootleg.events

import no.elg.infiniteBootleg.world.world.World

/**
 * Event fired when all the initial chunks of the world is loaded.
 */
data class InitialChunksOfWorldLoadedEvent(override val world: World) : WorldEvent
