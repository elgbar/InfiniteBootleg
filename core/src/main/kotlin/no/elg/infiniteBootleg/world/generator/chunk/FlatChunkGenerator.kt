package no.elg.infiniteBootleg.world.generator.chunk

import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.world.generator.biome.Biome
import no.elg.infiniteBootleg.world.world.World

/**
 * @author Elg
 */
class FlatChunkGenerator : ChunkGenerator {

  override fun generate(world: World, chunkX: Int, chunkY: Int): Chunk =
    ChunkImpl(world, chunkX, chunkY).also { chunk ->
      if (chunkY < 0) {
        for (x in 0 until Chunk.CHUNK_SIZE) {
          for (y in 0 until Chunk.CHUNK_SIZE) {
            chunk.setBlock(x, y, Material.STONE, updateTexture = false, prioritize = false, sendUpdatePacket = false)
          }
        }
      }
      chunk.finishLoading()
    }

  override fun getBiome(worldX: Int): Biome = Biome.PLAINS
  override fun getHeight(worldX: Int): Int = 0
  override val seed: Long = 0
}
