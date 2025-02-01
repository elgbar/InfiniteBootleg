package no.elg.infiniteBootleg.client.world.generator.chunk

import no.elg.infiniteBootleg.client.world.chunks.TexturedChunkImpl
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.world.generator.chunk.ChunkFactory
import no.elg.infiniteBootleg.core.world.world.World

class TexturedChunkFactory : ChunkFactory<TexturedChunkImpl> {

  override fun createChunk(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord): TexturedChunkImpl = TexturedChunkImpl(world, chunkX, chunkY)
}
