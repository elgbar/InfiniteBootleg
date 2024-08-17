package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.world.chunks.Chunk

class ChunkAddedToChunkRendererEvent(override val chunk: Chunk, val prioritized: Boolean) : ChunkEvent
