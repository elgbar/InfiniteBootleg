package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.biome.Biome;
import no.elg.infiniteBootleg.world.generator.simplex.SimplexNoise;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * @author Elg
 */
public class SimplexChunkGenerator implements ChunkGenerator {

    private final SimplexNoise elevationSim;

    public SimplexChunkGenerator(long seed) {
        elevationSim = new SimplexNoise(2, 23f, new Random(seed).nextInt());
    }


    @Override
    public @NotNull Chunk generate(@Nullable World world, @NotNull Location chunkPos, @NotNull Random random) {
        final float[] mat = {1.0f, 0.59f, 0.41f, 0.19f, 0.09f, 0.03f};
        double exp = 0.5;

        double nx = chunkPos.x / 128f - 0.5, ny = 128f - 0.5;
        double e = (mat[0] * Biome.noise(elevationSim, 1 * nx, 1 * ny) + mat[1] * Biome.noise(elevationSim, 2 * nx, 2 * ny) +
                    mat[2] * Biome.noise(elevationSim, 4 * nx, 4 * ny) + mat[3] * Biome.noise(elevationSim, 8 * nx, 8 * ny) +
                    mat[4] * Biome.noise(elevationSim, 16 * nx, 16 * ny) + mat[5] * Biome.noise(elevationSim, 32 * nx, 32 * ny));
        e /= (mat[0] + mat[1] + mat[2] + mat[3] + mat[4] + mat[5]);
        e = Math.pow(e, exp);

        System.out.println("e = " + e);

        if (e > 0) {
            return Biome.PLAINS.generate(world, chunkPos);
        }
        else {
            return Biome.ANCIENT_MOUNTAINS.generate(world, chunkPos);
        }
    }

    private void fillUpTo(Chunk chunk, int x, int y, Material mat) {
        for (int dy = 0; dy < y; dy++) {
            chunk.setBlock(x, dy, mat, false);
        }
    }
}
