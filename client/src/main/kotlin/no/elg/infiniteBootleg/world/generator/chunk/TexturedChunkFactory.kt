package no.elg.infiniteBootleg.world.generator.chunk

import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.world.chunks.TexturedChunkImpl
import no.elg.infiniteBootleg.world.world.World

class TexturedChunkFactory : ChunkFactory<TexturedChunkImpl> {

  override fun createChunk(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord): TexturedChunkImpl = TexturedChunkImpl(world, chunkX, chunkY)
}
