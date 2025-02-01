package no.elg.infiniteBootleg.core.events

import no.elg.infiniteBootleg.core.world.world.World

/**
 * Event fired when all the initial chunks of the world is loaded.
 */
data class InitialChunksOfWorldLoadedEvent(override val world: World) : WorldEvent
