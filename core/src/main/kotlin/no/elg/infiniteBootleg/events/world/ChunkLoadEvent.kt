package no.elg.infiniteBootleg.events.world

import no.elg.infiniteBootleg.events.Event
import no.elg.infiniteBootleg.world.Chunk

data class ChunkLoadEvent(val chunk: Chunk, override val async: Boolean) : Event
