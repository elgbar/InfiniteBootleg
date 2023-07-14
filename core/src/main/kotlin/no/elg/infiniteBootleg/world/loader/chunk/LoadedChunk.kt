package no.elg.infiniteBootleg.world.loader.chunk

import no.elg.infiniteBootleg.world.chunks.Chunk

data class LoadedChunk(val chunk: Chunk?, val isNewlyGenerated: Boolean)
