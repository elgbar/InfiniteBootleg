package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.biome.Biome;
import no.elg.infiniteBootleg.world.generator.noise.PerlinNoise;
import org.jetbrains.annotations.NotNull;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;

/**
 * TODO RESEARCH BILINEAR INTERPOLATION
 *
 * @author Elg
 */
public class PerlinChunkGenerator implements ChunkGenerator {

    private PerlinNoise noise;

    public PerlinChunkGenerator(int seed) {
        noise = new PerlinNoise(seed);
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
    public @NotNull Chunk generate(@NotNull World world, int chunkX, int chunkY) {
        Chunk chunk = new Chunk(world, chunkX, chunkY);
//        Main.SCHEDULER.executeAsync(() -> {
        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            int worldX = CoordUtil.chunkToWorld(chunkX, localX);
            Biome biome = getBiome(worldX);

            int worldY = biome.heightAt(this, worldX);

            int genChunkY = CoordUtil.worldToChunk(worldY);

            if (chunkY == genChunkY) {
                biome.fillUpTo(noise, chunk, localX, worldY - genChunkY * CHUNK_SIZE, worldY);
            }
            else if (chunkY < genChunkY) {
                biome.fillUpTo(noise, chunk, localX, CHUNK_SIZE, worldY);
            }
        }
        chunk.updateTexture(false);
//        });
        return chunk;
    }

    public PerlinNoise getNoise() {
        return noise;
    }
}
