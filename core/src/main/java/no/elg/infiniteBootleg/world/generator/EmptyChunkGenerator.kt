package no.elg.infiniteBootleg.world.generator

import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.ChunkImpl
import no.elg.infiniteBootleg.world.generator.biome.Biome
import no.elg.infiniteBootleg.world.world.World

/**
 * Generate chunks that are always air
 *
 * @author Elg
 */
class EmptyChunkGenerator : ChunkGenerator {
  override fun getBiome(worldX: Int): Biome {
    return Biome.PLAINS
  }

  override fun getHeight(worldX: Int): Int {
    return 0
  }

  override fun generate(world: World, chunkX: Int, chunkY: Int): Chunk {
    val chunk = ChunkImpl(world, chunkX, chunkY)
    chunk.finishLoading()
    return chunk
  }
}
