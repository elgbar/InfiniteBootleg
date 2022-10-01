package no.elg.infiniteBootleg.events

import no.elg.infiniteBootleg.events.api.AsyncEvent
import no.elg.infiniteBootleg.events.api.ThreadType
import no.elg.infiniteBootleg.world.World

/**
 * Event fired when all the initial chunks of the world is loaded.
 */
data class InitialChunksOfWorldLoadedEvent(val world: World) : AsyncEvent(ThreadType.RENDER)
