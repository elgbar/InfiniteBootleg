package no.elg.infiniteBootleg.core.world.generator.chunk

import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.core.world.world.World

class ChunkImplFactory : ChunkFactory<ChunkImpl> {
  override fun createChunk(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord): ChunkImpl = ChunkImpl(world, chunkX, chunkY)
}
