package no.elg.infiniteBootleg.world.generator.chunk

import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.generator.biome.Biome
import no.elg.infiniteBootleg.world.world.World

/**
 * Generate chunks that are always air
 *
 * @author Elg
 */
class EmptyChunkGenerator : ChunkGenerator {

  override fun generate(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord): Chunk =
    Main.inst().chunkFactory.createChunk(world, chunkX, chunkY).also {
      it.finishLoading()
    }

  override fun getBiome(worldX: WorldCoord): Biome = Biome.PLAINS
  override fun getHeight(worldX: WorldCoord): Int = 0
  override val seed: Long = 0
}
