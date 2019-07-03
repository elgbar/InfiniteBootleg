package no.elg.infiniteBootleg.world.generator.biome;

import no.elg.infiniteBootleg.util.Tuple;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator;
import no.elg.infiniteBootleg.world.generator.noise.PerlinNoise;

import java.util.ArrayList;
import java.util.List;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;

/**
 * @author Elg
 */
public enum Biome {

    PLAINS(0.1, 0.9, 1, 64, 0.009, 0, Material.STONE, new Tuple<>(Material.TORCH, 1), new Tuple<>(Material.GRASS, 1),
           new Tuple<>(Material.DIRT, 10)),
    MOUNTAINS(100, 0.9, 1, 356, 0.005, 100, Material.STONE, new Tuple<>(Material.TORCH, 1), new Tuple<>(Material.GRASS, 1),
              new Tuple<>(Material.DIRT, 6)),
    DESERT(0.1, 0.9, 0.9, 32, 0.005, 0, Material.STONE, new Tuple<>(Material.SAND, 12));

    public final double y;
    public final double z;
    public final double exponent;
    public final double amplitude;
    public final double frequency;
    private final int offset;
    public final Material filler;
    public final Material[] topSoil;

    public static final int AVERAGE_RADIUS = 5;

    @SafeVarargs
    Biome(double y, double z, double exponent, double amplitude, double frequency, int offset, Material filler,
          Tuple<Material, Integer>... topSoil) {
        this.y = y;
        this.z = z;
        this.exponent = exponent;
        this.amplitude = amplitude;
        this.frequency = frequency;
        this.offset = offset;
        this.filler = filler;
        List<Material> mats = new ArrayList<>();
        for (Tuple<Material, Integer> tuple : topSoil) {
            for (int i = 0; i < tuple.value; i++) {
                mats.add(tuple.key);
            }
        }
        this.topSoil = mats.toArray(new Material[0]);
    }

    public Material materialAt(PerlinNoise noise, int height, int worldY) {
        int delta = height - worldY - 2;
        int d = (int) noise.noise(delta, 0.1, 0.3, height, 0.1);

        if (delta - d > 0) {
            delta -= d;
        }

        if (delta < 0) { return null; }
        if (delta >= topSoil.length) {
            return filler;
        }
        else {
            return topSoil[delta];
        }
    }

    public int avgHeightAt(PerlinChunkGenerator pcg, int worldX) {
        if (worldX == 375) {
            System.out.println("375");
        }
        int y = 0;
        for (int dx = -AVERAGE_RADIUS; dx <= AVERAGE_RADIUS; dx++) {
            if (dx != 0) {
                Biome biome = pcg.getBiome(worldX + dx);
                y += biome.rawHeightAt(pcg.getNoise(), worldX);
            }
            else {
                y += rawHeightAt(pcg.getNoise(), worldX);
            }
        }
        return Math.abs((int) Math.floor(y / (AVERAGE_RADIUS * 2 + 1)));
    }

    public double rawHeightAt(PerlinNoise noise, int worldX) {
        return rawHeightAt(noise, worldX, y, z, amplitude, frequency, offset);
    }

    private static double rawHeightAt(PerlinNoise noise, int worldX, double y, double z, double amplitude, double frequency,
                                      int offset) {
//        int lx = localX + CHUNK_SIZE * chunkX;
        return noise.octaveNoise(worldX * frequency, y * frequency, z * frequency, 6, 0.5) * amplitude + offset;
//        return ImprovedNoise.noise(localX + CHUNK_SIZE * chunkPos.x, y, z, amplitude, frequency);
//        double nx = ;

//        double e = (weights[0] * + weights[1] * noise(noise, 2 * nx, 2 * ny) +
//                    weights[2] * noise(noise, 4 * nx, 4 * ny) + weights[3] * noise(noise, 8 * nx, 8 * ny) +
//                    weights[4] * noise(noise, 16 * nx, 16 * ny) + weights[5] * noise(noise, 32 * nx, 32 * ny));
//        e /= (weights[0] + weights[1] + weights[2] + weights[3] + weights[4] + weights[5]);
//        return ImprovedNoise.noise(localX + CHUNK_SIZE * chunkPos.x, y, z, amplitude, frequency);
//        double e = noise
//            .getOctNoise((localX + CHUNK_SIZE * chunkPos.x), (int) (weights[0] * weights[1]), (int) (weights[2] * weights[3]));
//        e = Math.pow(e, exponent);
//        System.out.println("e = " + e);
//        return (int) (e * intensity);
    }

    public void fillUpTo(PerlinNoise noise, Chunk chunk, int localX, int localY, int height) {
        int chunkY = chunk.getLocation().y * CHUNK_SIZE;
        for (int dy = 0; dy < localY; dy++) {
            chunk.setBlock(localX, dy, materialAt(noise, height, dy + chunkY), false);
        }
    }
}
