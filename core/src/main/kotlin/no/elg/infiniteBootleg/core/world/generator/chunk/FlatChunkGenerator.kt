package no.elg.infiniteBootleg.core.world.generator.chunk

import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.generator.biome.Biome
import no.elg.infiniteBootleg.core.world.world.World

/**
 * @author Elg
 */
class FlatChunkGenerator : ChunkGenerator {

  override fun generate(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord): Chunk =
    Main.Companion.inst().chunkFactory.createChunk(world, chunkX, chunkY).also { chunk ->
      if (chunkY < 0) {
        for (localX in 0 until Chunk.Companion.CHUNK_SIZE) {
          for (localY in 0 until Chunk.Companion.CHUNK_SIZE) {
            val block = Material.Stone.createBlock(world, chunk, localX, localY, tryRevalidateChunk = false)
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
