package no.elg.infiniteBootleg.core.world.generator.chunk

import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.generator.biome.Biome
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.ProtoWorld

/**
 * Generates chunks from world coordinates
 *
 * @author Elg
 */
interface ChunkGenerator {

  val seed: Long

  /**
   * @param worldX World location
   * @return The biome at the calculated location
   */
  fun getBiome(worldX: WorldCoord): Biome

  /**
   * @param worldX World location
   * @return The world height at this location
   */
  fun getHeight(worldX: WorldCoord): Int

  /**
   * @param world The world to generate the chunk in
   * @param chunkX X coordinate of chunk in world to generate
   * @param chunkY Y coordinate of chunk in world to generate
   * @return A valid chunk at the given offset in the given world
   */
  fun generate(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord): Chunk

  fun generateFeatures(chunk: Chunk) {}

  companion object {
    fun getGeneratorType(generator: ChunkGenerator?): ProtoWorld.World.Generator =
      when (generator) {
        is PerlinChunkGenerator -> ProtoWorld.World.Generator.PERLIN
        is FlatChunkGenerator -> ProtoWorld.World.Generator.FLAT
        is EmptyChunkGenerator -> ProtoWorld.World.Generator.EMPTY
        else -> ProtoWorld.World.Generator.UNRECOGNIZED
      }
  }
}
