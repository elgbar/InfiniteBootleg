package no.elg.infiniteBootleg.events

import no.elg.infiniteBootleg.events.api.Event
import no.elg.infiniteBootleg.world.Chunk

data class ChunkLoadEvent(val chunk: Chunk, override val async: Boolean) : Event
