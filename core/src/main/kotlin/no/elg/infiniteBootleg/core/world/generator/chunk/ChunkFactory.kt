package no.elg.infiniteBootleg.core.world.generator.chunk

import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.core.world.world.World

interface ChunkFactory<C : ChunkImpl> {

  /**
   * Create a new chunk for the given world at the given chunk coordinates
   */
  fun createChunk(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord): C
}
