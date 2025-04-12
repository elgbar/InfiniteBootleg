package no.elg.infiniteBootleg.core.world.generator.chunk

import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.generator.biome.Biome
import no.elg.infiniteBootleg.core.world.world.World

/**
 * Generate chunks that are always air
 *
 * @author Elg
 */
class EmptyChunkGenerator : ChunkGenerator {

  override fun generate(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord): Chunk =
    Main.Companion.inst().chunkFactory.createChunk(world, chunkX, chunkY).also {
      it.finishLoading()
    }

  override fun getBiome(worldX: WorldCoord): Biome = Biome.Plains
  override fun getHeight(worldX: WorldCoord): Int = 0
  override val seed: Long = 0
}
