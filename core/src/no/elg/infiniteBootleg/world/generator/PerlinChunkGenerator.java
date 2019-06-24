package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
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

    private double calcHeightMap(int chunkX, int x) {
        float a = 1.25f;
        return (noise.noise(chunkX * CHUNK_SIZE + x, 0.5, 0.5, a, 0.001) + a) / (a * 2);
    }

    private Biome getBiome(double height) {
        if (height > 0.5) {
            return Biome.ANCIENT_MOUNTAINS;
        }
        else {
            return Biome.PLAINS;
        }
    }

    @Override
    public @NotNull Chunk generate(@NotNull World world, @NotNull Location chunkPos) {
        Chunk chunk = new Chunk(world, chunkPos);
//        Main.SCHEDULER.executeAsync(() -> {
        for (int x = 0; x < CHUNK_SIZE; x++) {
            double biomeWeight = calcHeightMap(chunkPos.x, x);
            Biome biome = getBiome(biomeWeight);
            double y;

            y = biome.heightAt(noise, chunkPos.x, x) * biomeWeight;

            int height = (int) y;
            int elevationChunk = CoordUtil.worldToChunk(height);
            if (chunkPos.y == elevationChunk) {
                biome.fillUpTo(noise, chunk, x, (int) (y - elevationChunk * CHUNK_SIZE), height);
            }
            else if (chunkPos.y < elevationChunk) {
                biome.fillUpTo(noise, chunk, x, CHUNK_SIZE, height);
            }
        }
        chunk.updateTexture(false);
//        });
        return chunk;
    }
}
