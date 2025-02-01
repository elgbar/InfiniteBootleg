package no.elg.infiniteBootleg.core.world.loader.chunk

import no.elg.infiniteBootleg.core.world.chunks.Chunk

data class LoadedChunk(val chunk: Chunk?, val isNewlyGenerated: Boolean)
