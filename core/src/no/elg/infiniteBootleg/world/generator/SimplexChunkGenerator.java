package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.biome.Biome;
import no.elg.infiniteBootleg.world.generator.simplex.ImprovedNoise;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_HEIGHT;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_WIDTH;

/**
 * @author Elg
 */
public class SimplexChunkGenerator implements ChunkGenerator {

    //    private final OctavePerlin eelevationSim;
//
    public SimplexChunkGenerator(int seed) {
//        elevationSim = new octaveNoise(555, 2, seed);
    }

    Biome currBiome = Biome.PLAINS;

    private double calcHeightMap(int chunkX, int x) {
        int a = 1;
        double biomeWeight = (ImprovedNoise.noise(chunkX * CHUNK_WIDTH + x, 0.5, 0.5, a, 0.001) + a) / 2;
//            biomeWeight += ImprovedNoise.noise(0.5, chunkPos.x * CHUNK_WIDTH + x, 0.5, a / 4, 0.005);
        return biomeWeight;
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
    public @NotNull Chunk generate(@Nullable World world, @NotNull Location chunkPos, @NotNull Random random) {
        Chunk chunk = new Chunk(world, chunkPos);
//        Main.SCHEDULER.executeAsync(() -> {
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            double biomeWeight = calcHeightMap(chunkPos.x, x);
            Biome biome = getBiome(biomeWeight);
            double y;

            y = biome.heightAt(chunkPos.x, x) * biomeWeight;


//
//            double pWeight = Biome.PLAINS.heightAt(chunkPos, x) * biomeWeight * 2;
//            y += pWeight;
//            double ahWeight = Biome.ANCIENT_MOUNTAINS.heightAt(chunkPos, x) * (1 - biomeWeight);
//            y += ahWeight;
//            y /= 2;
//
//            System.out.println("pWeight = " + pWeight);
//            System.out.println("ahWeight = " + ahWeight);
////
////
//            if (pWeight <= ahWeight) {
//                biome = Biome.ANCIENT_MOUNTAINS;
//            }

            int height = (int) y;
            int elevationChunk = CoordUtil.worldToChunk(height);
            if (chunkPos.y == elevationChunk) {
                biome.fillUpTo(chunk, x, (int) (y - elevationChunk * CHUNK_WIDTH), height);
            }
            else if (chunkPos.y < elevationChunk) {
                biome.fillUpTo(chunk, x, CHUNK_HEIGHT, height);
            }
        }
        chunk.update(false);
//        });
        return chunk;
    }
}
