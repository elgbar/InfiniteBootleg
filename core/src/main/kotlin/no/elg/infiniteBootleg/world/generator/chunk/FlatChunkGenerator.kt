package no.elg.infiniteBootleg.world.generator.chunk

import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.generator.biome.Biome
import no.elg.infiniteBootleg.world.world.World

/**
 * @author Elg
 */
class FlatChunkGenerator : ChunkGenerator {

  override fun generate(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord): Chunk =
    Main.inst().chunkFactory.createChunk(world, chunkX, chunkY).also { chunk ->
      if (chunkY < 0) {
        for (localX in 0 until Chunk.CHUNK_SIZE) {
          for (localY in 0 until Chunk.CHUNK_SIZE) {
            val block = Material.STONE.createBlock(world, chunk, localX, localY, tryRevalidateChunk = false)
            chunk.setBlock(localX, localY, block, updateTexture = false, prioritize = false, sendUpdatePacket = false)
          }
        }
      }
      chunk.finishLoading()
    }

  override fun getBiome(worldX: WorldCoord): Biome = Biome.PLAINS
  override fun getHeight(worldX: WorldCoord): Int = 0
  override val seed: Long = 0
}
