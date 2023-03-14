package no.elg.infiniteBootleg.world.generator;

import static no.elg.infiniteBootleg.protobuf.ProtoWorld.World.Generator.EMPTY;
import static no.elg.infiniteBootleg.protobuf.ProtoWorld.World.Generator.FLAT;
import static no.elg.infiniteBootleg.protobuf.ProtoWorld.World.Generator.PERLIN;
import static no.elg.infiniteBootleg.protobuf.ProtoWorld.World.Generator.UNRECOGNIZED;

import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.generator.biome.Biome;
import no.elg.infiniteBootleg.world.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * Generates chunks from world coordinates
 *
 * @author Elg
 */
public interface ChunkGenerator {

  /**
   * @param worldX World location
   * @return The biome at the calculated location
   */
  @NotNull
  Biome getBiome(int worldX);

  /**
   * @param worldX World location
   * @return The world height at this location
   */
  int getHeight(int worldX);

  /**
   * @param world The world to generate the chunk in
   * @param chunkX X coordinate of chunk in world to generate
   * @param chunkY Y coordinate of chunk in world to generate
   * @return A valid chunk at the given offset in the given world
   */
  @NotNull
  Chunk generate(@NotNull World world, int chunkX, int chunkY);

  static ProtoWorld.World.Generator getGeneratorType(ChunkGenerator generator) {
    if (generator instanceof PerlinChunkGenerator) {
      return PERLIN;
    } else if (generator instanceof FlatChunkGenerator) {
      return FLAT;
    } else if (generator instanceof EmptyChunkGenerator) {
      return EMPTY;
    } else {
      return UNRECOGNIZED;
    }
  }
}
