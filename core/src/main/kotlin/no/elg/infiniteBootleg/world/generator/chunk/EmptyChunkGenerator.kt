package no.elg.infiniteBootleg.world.generator.chunk

import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.world.generator.biome.Biome
import no.elg.infiniteBootleg.world.world.World

/**
 * Generate chunks that are always air
 *
 * @author Elg
 */
class EmptyChunkGenerator : ChunkGenerator {

  override fun generate(world: World, chunkX: Int, chunkY: Int): Chunk =
    ChunkImpl(world, chunkX, chunkY).also {
      it.finishLoading()
    }

  override fun getBiome(worldX: Int): Biome = Biome.PLAINS
  override fun getHeight(worldX: Int): Int = 0
  override val seed: Long = 0
}
