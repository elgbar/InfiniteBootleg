package no.elg.infiniteBootleg.world.generator;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;

import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.util.FastNoise;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.ChunkImpl;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.biome.Biome;
import no.elg.infiniteBootleg.world.generator.noise.PerlinNoise;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class PerlinChunkGenerator implements ChunkGenerator {

  private static final float CAVE_CREATION_THRESHOLD =
      0.92f; // noise values above this value will be cave (ie air)
  private static final float WORM_SIZE_AMPLITUDE =
      0.15f; // how much the size of the caves(worms) changes
  private static final float WORM_SIZE_FREQUENCY =
      0.1f; // how fast the size of the caves(worms) changes
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

  @Override
  public @NotNull Biome getBiome(int worldX) {

    float a = 1.25f;
    float height = (noise.noise(worldX, 0.5f, 0.5f, a, 0.001f) + a) / (a * 2);

    if (height > 0.7) {
      return Biome.MOUNTAINS;
    } else if (height > 0.5) {
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
      int worldX = CoordUtil.chunkToWorld(chunkX, localX);
      Biome biome = getBiome(worldX);

      int genHeight = biome.heightAt(this, worldX);

      int genChunkY = CoordUtil.worldToChunk(genHeight);

      if (chunkY == genChunkY) {
        biome.fillUpTo(noise, chunk, localX, genHeight - genChunkY * CHUNK_SIZE, genHeight);
      } else if (chunkY < genChunkY) {
        biome.fillUpTo(noise, chunk, localX, CHUNK_SIZE, genHeight);
      }

      // generate caves
      int worldChunkY = CoordUtil.chunkToWorld(chunkY);
      for (int localY = 0; localY < CHUNK_SIZE; localY++) {
        int worldY = worldChunkY + localY;

        // calculate the size of the worm
        float wormSize =
            1 + Math.abs(noise.noise(worldX, worldY, 1, WORM_SIZE_AMPLITUDE, WORM_SIZE_FREQUENCY));
        float x = noise2.GetNoise(worldX, worldY) / wormSize;
        if (x > CAVE_CREATION_THRESHOLD) {
          //                        Material mat = x > 0.99 && chunkY < genChunkY ? Material.TORCH :
          // null;
          //                        Block b = mat == null ? null : mat.createBlock(world, chunk,
          // localX, localY);
          chunk.getBlocks()[localX][localY] = null;
        }
      }
    }
    chunk.finishLoading();
    return chunk;
  }
}
