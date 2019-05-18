package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.simplex.SimplexNoise;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_HEIGHT;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_WIDTH;

/**
 * @author Elg
 */
public class SimplexChunkGenerator implements ChunkGenerator {

    private final SimplexNoise elevationSim;
    private final SimplexNoise detailSim;
    //    private final SimplexNoise roughnessSim;
    private final Random random;

    public SimplexChunkGenerator(long seed) {
        random = new Random(seed);
        elevationSim = new SimplexNoise(8, 0.85f, random.nextInt());
        detailSim = new SimplexNoise(256, 0.45f, random.nextInt());

//        var rng1 = PM_PRNG.create(seed1);
//        var rng2 = PM_PRNG.create(seed2);
//        var gen1 = new SimplexNoise(rng1.nextDouble.bind(rng1));
//        var gen2 = new SimplexNoise(rng2.nextDouble.bind(rng2));
//        function noise(elevationSim,nx, ny) { return gen1.detailSimD(nx, ny)/2 + 0.5; }
//        function noise(detailSim,nx, ny) { return gen2.detailSimD(nx, ny)/2 + 0.5; }
//
//        for (var y = 0; y < height; y++) {
//            for (var x = 0; x < width; x++) {
//                var nx = x/width - 0.5, ny = y/height - 0.5;
//        var e = (1.00 * noise(elevationSim, 1 * nx,  1 * ny)
//                 + 0.59 * noise(elevationSim, 2 * nx,  2 * ny)
//                 + 0.41 * noise(elevationSim, 4 * nx,  4 * ny)
//                 + 0.19 * noise(elevationSim, 8 * nx,  8 * ny)
//                 + 0.09 * noise(elevationSim,16 * nx, 16 * ny)
//                 + 0.00 * noise(elevationSim,32 * nx, 32 * ny));
//        e /= (1.00+0.59+0.41+0.19+0.09+0.00);
//        e = Math.pow(e, 0.50);
//        var m = (1.00 * noise(detailSim, 1 * nx,  1 * ny)
//                 + 0.69 * noise(detailSim, 2 * nx,  2 * ny)
//                 + 0.45 * noise(detailSim, 4 * nx,  4 * ny)
//                 + 0.28 * noise(detailSim, 8 * nx,  8 * ny)
//                 + 0.13 * noise(detailSim,16 * nx, 16 * ny)
//                 + 0.02 * noise(detailSim,32 * nx, 32 * ny));
//        m /= (1.00+0.69+0.45+0.28+0.13+0.02);
        /* draw biome(e, m) at x,y */
//            }
//        }

    }

    double noise(SimplexNoise noise, double nx, double ny) { return noise.getNoise(nx, ny) / 2 + 0.5; }

    @Override
    public @NotNull Chunk generate(@Nullable World world, @NotNull Location chunkPos, @NotNull Random random) {
        Chunk chunk = new Chunk(world, chunkPos);

        for (int x = 0; x < CHUNK_WIDTH; x++) {

            double nx = x / 128f - 0.5 + chunkPos.x * CHUNK_WIDTH, ny = 128f - 0.5 + chunkPos.x * CHUNK_WIDTH;
            double e = (1.00 * noise(elevationSim, 1 * nx, 1 * ny) + 0.59 * noise(elevationSim, 2 * nx, 2 * ny) +
                        0.41 * noise(elevationSim, 4 * nx, 4 * ny) + 0.19 * noise(elevationSim, 8 * nx, 8 * ny) +
                        0.09 * noise(elevationSim, 16 * nx, 16 * ny) + 0.03 * noise(elevationSim, 32 * nx, 32 * ny));
            e /= (1.00 + 0.59 + 0.41 + 0.19 + 0.09 + 0.03);
            e = Math.pow(e, 0.50);

//                        double m = (1.00 * noise(detailSim, 1 * nx, 1 * ny) + 0.69 * noise(detailSim, 2 * nx, 2 * ny) +
//                        0.45 * noise(detailSim, 4 * nx, 4 * ny) + 0.28 * noise(detailSim, 8 * nx, 8 * ny) +
//                        0.13 * noise(detailSim, 16 * nx, 16 * ny) + 0.02 * noise(detailSim, 32 * nx, 32 * ny));
//            m /= (1.00 + 0.69 + 0.45 + 0.28 + 0.13 + 0.02);
//
//            System.out.println("\n\n\n\nx = " + x);
//            System.out.println("e = " + e);
//            System.out.println("m = " + m);

            int elevation = (int) (e * 1280);
            int elevationChunk = CoordUtil.worldToChunk(elevation);//elevation - (CHUNK_HEIGHT * chunkPos.y) - 1;
            System.out.println("pos = " + (x + CHUNK_WIDTH * chunkPos.x) + ", " + elevationChunk);
            if (chunkPos.y == elevationChunk) {
                fillUpTo(chunk, x, elevation - elevationChunk * CHUNK_WIDTH, Material.STONE);
            }
            else if (chunkPos.y < elevationChunk) {
                fillUpTo(chunk, x, CHUNK_HEIGHT, Material.STONE);
            }
//            System.out.println("elevation = " + elevation);
//            if (elevation > CHUNK_HEIGHT * (chunkPos.y - 1)) {
//                fillUpTo(chunk, x, CHUNK_HEIGHT, Material.STONE);
//            }
//            else if (elevation < CHUNK_HEIGHT * (chunkPos.y + 1)) {
//                //DO NOTHING
//            }
//            else {
//                System.out.println("easfsef");
//                int realY = (int) (elevation - CHUNK_HEIGHT * chunkPos.y);
//                fillUpTo(chunk, x, realY, Material.STONE);
//            }
        }
        chunk.update(false);
        return chunk;
    }

    private void fillUpTo(Chunk chunk, int x, int y, Material mat) {
        for (int dy = 0; dy < y; dy++) {
            chunk.setBlock(x, dy, mat, false);
        }
    }
}
