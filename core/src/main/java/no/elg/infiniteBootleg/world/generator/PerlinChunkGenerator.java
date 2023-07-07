package no.elg.infiniteBootleg.world.generator;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;

import no.elg.infiniteBootleg.util.CoordUtilKt;
import no.elg.infiniteBootleg.util.FastNoise;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.ChunkImpl;
import no.elg.infiniteBootleg.world.generator.biome.Biome;
import no.elg.infiniteBootleg.world.generator.noise.PerlinNoise;
import no.elg.infiniteBootleg.world.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class PerlinChunkGenerator implements ChunkGenerator {

  /** Noise values above this value will be cave (i.e., air). */
  private static final double CAVE_CREATION_THRESHOLD = 0.92;
  /** How much the size of the caves (worms) changes */
  private static final double WORM_SIZE_AMPLITUDE = 0.15;
  /** How fast the size of the caves (worms) changes */
  private static final double WORM_SIZE_FREQUENCY = 0.1;

  /** How many blocks of the surface should not be caved in */
  private static final double CAVELESS_DEPTH = 16;

  private final PerlinNoise noise;
  private final FastNoise noise2;

  public PerlinChunkGenerator(long seed) {
    noise = new PerlinNoise(seed);
    noise2 = new FastNoise((int) seed);

    noise2.SetNoiseType(FastNoise.NoiseType.PerlinFractal);
    noise2.SetFrequency(0.01f);
    noise2.SetInterp(FastNoise.Interp.Quintic);

    noise2.SetFractalType(FastNoise.FractalType.RigidMulti);
    noise2.SetFractalOctaves(1);
    noise2.SetFractalLacunarity(1.0f);
    noise2.SetFractalGain(0.5f);
  }

  public PerlinNoise getNoise() {
    return noise;
  }

  public double getBiomeHeight(int worldX) {
    double a = 1.25d;
    return (noise.noise(worldX, 0.5d, 0.5d, a, 0.001d) + a) / (a * 2);
  }

  @Override
  public @NotNull Biome getBiome(int worldX) {
    double height = getBiomeHeight(worldX);
    if (height > 0.65) {
      return Biome.MOUNTAINS;
    } else if (height > 0.45) {
      return Biome.PLAINS;
    } else if (height > 0.15) {
      return Biome.DESERT;
    } else {
      return Biome.PLAINS;
    }
  }

  @Override
  public int getHeight(int worldX) {
    return getBiome(worldX).heightAt(this, worldX);
  }

  @NotNull
  @Override
  public Chunk generate(@NotNull World world, int chunkX, int chunkY) {

    ChunkImpl chunk = new ChunkImpl(world, chunkX, chunkY);
    for (int localX = 0; localX < CHUNK_SIZE; localX++) {
      int worldX = CoordUtilKt.chunkToWorld(chunkX, localX);
      Biome biome = getBiome(worldX);

      int genHeight = biome.heightAt(this, worldX);

      int genChunkY = CoordUtilKt.worldToChunk(genHeight);

      if (chunkY == genChunkY) {
        biome.fillUpTo(noise, chunk, localX, CoordUtilKt.chunkOffset(genHeight) + 1, genHeight);
      } else if (chunkY < genChunkY) {
        biome.fillUpTo(noise, chunk, localX, CHUNK_SIZE, genHeight);
      }

      // generate caves (where there is something to generate them in
      if (chunkY <= genChunkY) {
        Block[][] blocks = chunk.getBlocks();
        int worldChunkY = CoordUtilKt.chunkToWorld(chunkY);
        for (int localY = 0; localY < CHUNK_SIZE; localY++) {
          int worldY = worldChunkY + localY;

          // calculate the size of the worm
          double wormSize =
              1
                  + Math.abs(
                      noise.noise(worldX, worldY, 1, WORM_SIZE_AMPLITUDE, WORM_SIZE_FREQUENCY));
          double caveNoise = noise2.GetNoise(worldX, worldY) / wormSize;
          double diffToSurface = genHeight - worldY;
          double depthModifier = Math.min(1f, diffToSurface / CAVELESS_DEPTH);
          if (caveNoise > CAVE_CREATION_THRESHOLD / depthModifier) {
            blocks[localX][localY] = null;
          }
        }
      }
    }
    chunk.finishLoading();
    return chunk;
  }
}
