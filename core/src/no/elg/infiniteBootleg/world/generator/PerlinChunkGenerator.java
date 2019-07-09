package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.util.FastNoise;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.biome.Biome;
import no.elg.infiniteBootleg.world.generator.noise.PerlinNoise;
import org.jetbrains.annotations.NotNull;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;

/**
 * @author Elg
 */
public class PerlinChunkGenerator implements ChunkGenerator {

    private final PerlinNoise noise;
    private final FastNoise noise2;

    public PerlinChunkGenerator(int seed) {
        noise = new PerlinNoise(seed);
        noise2 = new FastNoise(seed);

        noise2.SetNoiseType(FastNoise.NoiseType.PerlinFractal);
        noise2.SetFrequency(0.01f);
        noise2.SetInterp(FastNoise.Interp.Quintic);


        noise2.SetFractalType(FastNoise.FractalType.RigidMulti);
        noise2.SetFractalOctaves(1);
        noise2.SetFractalLacunarity(1.0f);
        noise2.SetFractalGain(0.5f);
    }

    @Override
    public @NotNull Biome getBiome(int worldX) {

        float a = 1.25f;
        double height = (noise.noise(worldX, 0.5, 0.5, a, 0.001) + a) / (a * 2);

        if (height > 0.7) {
            return Biome.MOUNTAINS;
        }
        else if (height > 0.5) {
            return Biome.DESERT;
        }
        else {
            return Biome.PLAINS;
        }
    }

    @Override
    public int getHeight(int worldX) {
        return getBiome(worldX).heightAt(this, worldX);
    }

    @Override
    public @NotNull Chunk generate(@NotNull World world, int chunkX, int chunkY) {
        Chunk chunk = new Chunk(world, chunkX, chunkY);
//        Main.SCHEDULER.executeAsync(() -> {
        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            int worldX = CoordUtil.chunkToWorld(chunkX, localX);
            Biome biome = getBiome(worldX);

            int genHeight = biome.heightAt(this, worldX);

            int genChunkY = CoordUtil.worldToChunk(genHeight);

            if (chunkY == genChunkY) {
                biome.fillUpTo(noise, chunk, localX, genHeight - genChunkY * CHUNK_SIZE, genHeight);
            }
            else if (chunkY < genChunkY) {
                biome.fillUpTo(noise, chunk, localX, CHUNK_SIZE, genHeight);
            }

            //generate caves
            int worldChunkY = CoordUtil.chunkToWorld(chunkY);
            for (int localY = 0; localY < CHUNK_SIZE; localY++) {
                int worldY = worldChunkY + localY;

                double wormSize = 1 + Math.abs(noise.noise(worldX, worldY, 1, 0.15f, 0.01f));
//                System.out.println("y -> wormSize = " + worldY + " -> " + wormSize);

                double x = noise2.GetNoise(worldX, worldY) / wormSize;
//                System.out.println("n:y" + x + ":" + worldY);
                if (x > 0.95) {
                    chunk.setBlock(localX, localY, x > 0.99 && chunkY < genChunkY ? Material.TORCH : null, false);
                }
            }

        }
//        chunk.updateTexture(false);
//        });
        return chunk;
    }

    public PerlinNoise getNoise() {
        return noise;
    }
}
