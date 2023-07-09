package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.world.Chunk

class ChunkTextureChangedEvent(override val chunk: Chunk) : ChunkEvent
