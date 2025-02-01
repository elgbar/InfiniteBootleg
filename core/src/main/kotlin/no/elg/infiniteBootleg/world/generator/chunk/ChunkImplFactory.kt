package no.elg.infiniteBootleg.world.generator.chunk

import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.world.world.World

class ChunkImplFactory : ChunkFactory<ChunkImpl> {
  override fun createChunk(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord): ChunkImpl = ChunkImpl(world, chunkX, chunkY)
}
