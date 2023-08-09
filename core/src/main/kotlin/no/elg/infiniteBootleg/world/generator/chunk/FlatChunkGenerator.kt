package no.elg.infiniteBootleg.world.generator.chunk

import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.world.generator.biome.Biome
import no.elg.infiniteBootleg.world.world.World

/**
 * @author Elg
 */
class FlatChunkGenerator : ChunkGenerator {

  override fun generate(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord): Chunk =
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

  override fun getBiome(worldX: WorldCoord): Biome = Biome.PLAINS
  override fun getHeight(worldX: WorldCoord): Int = 0
  override val seed: Long = 0
}
