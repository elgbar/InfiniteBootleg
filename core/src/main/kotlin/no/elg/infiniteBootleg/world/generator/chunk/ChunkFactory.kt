package no.elg.infiniteBootleg.world.generator.chunk

import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.world.world.World

interface ChunkFactory<C : ChunkImpl> {

  /**
   * Create a new chunk for the given world at the given chunk coordinates
   */
  fun createChunk(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord): C
}
