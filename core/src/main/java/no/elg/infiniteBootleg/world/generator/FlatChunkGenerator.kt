package no.elg.infiniteBootleg.world.generator

import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.ChunkImpl
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.generator.biome.Biome
import no.elg.infiniteBootleg.world.world.World

/**
 * @author Elg
 */
class FlatChunkGenerator : ChunkGenerator {
  override fun getBiome(worldX: Int): Biome {
    return Biome.PLAINS
  }

  override fun getHeight(worldX: Int): Int {
    return 0
  }

  override fun generate(world: World, chunkX: Int, chunkY: Int): Chunk {
    val chunk = ChunkImpl(world, chunkX, chunkY)
    if (chunkY < 0) {
      for (x in 0 until Chunk.CHUNK_SIZE) {
        for (y in 0 until Chunk.CHUNK_SIZE) {
          chunk.setBlock(x, y, Material.STONE, false, false, false)
        }
      }
    }
    chunk.finishLoading()
    return chunk
  }
}
