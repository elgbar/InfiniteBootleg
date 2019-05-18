package no.elg.infiniteBootleg.world.generator.biome;

import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.simplex.SimplexNoise;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_HEIGHT;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_WIDTH;

/**
 * @author Elg
 */
public enum Biome {

    PLAINS(new SimplexNoise(8, 0.85f, 123), new float[] {1.0f, 0.59f, 0.41f, 0.19f, 0.09f, 0.03f}, 0.5, 256, Material.STONE),
    ANCIENT_MOUNTAINS(new SimplexNoise(2, 2f, 123), new float[] {1.0f, 1.0f, 0.31f, 0.16f, 0.07f, 0.04f}, 1.59, 1024,
                      Material.BRICK),
    ;

    private final SimplexNoise noise;
    public final float[] weights;
    public final double exponent;
    public final int intensity;
    private final Material filler;

    Biome(SimplexNoise noise, float[] weights, double exponent, int intensity, Material filler) {
        this.noise = noise;

        this.weights = weights;
        this.exponent = exponent;
        this.intensity = intensity;
        this.filler = filler;
    }

    public static double noise(SimplexNoise noise, double nx, double ny) { return noise.getNoise(nx, ny) / 2 + 0.5; }

    public Chunk generate(World world, Location chunkPos) {
        Chunk chunk = new Chunk(world, chunkPos);
        for (int x = 0; x < CHUNK_WIDTH; x++) {

            double nx = (x + CHUNK_WIDTH * chunkPos.x) / 128f - 0.5, ny = 128f - 0.5;
            double e = (weights[0] * noise(noise, 1 * nx, 1 * ny) + weights[1] * noise(noise, 2 * nx, 2 * ny) +
                        weights[2] * noise(noise, 4 * nx, 4 * ny) + weights[3] * noise(noise, 8 * nx, 8 * ny) +
                        weights[4] * noise(noise, 16 * nx, 16 * ny) + weights[5] * noise(noise, 32 * nx, 32 * ny));
            e /= (weights[0] + weights[1] + weights[2] + weights[3] + weights[4] + weights[5]);
            e = Math.pow(e, exponent) / 4;

            int elevation = (int) (e * intensity);
            int elevationChunk = CoordUtil.worldToChunk(elevation);
//            System.out.println("pos = " + (x + CHUNK_WIDTH * chunkPos.x) + ", " + elevationChunk);
            if (chunkPos.y == elevationChunk) {
                fillUpTo(chunk, x, elevation - elevationChunk * CHUNK_WIDTH, filler);
            }
            else if (chunkPos.y < elevationChunk) {
                fillUpTo(chunk, x, CHUNK_HEIGHT, filler);
            }
        }
        return chunk;
    }

    public static void fillUpTo(Chunk chunk, int x, int y, Material mat) {
        for (int dy = 0; dy < y; dy++) {
            chunk.setBlock(x, dy, mat, false);
        }
    }
}
