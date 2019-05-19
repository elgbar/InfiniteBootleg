package no.elg.infiniteBootleg.world.generator.biome;

import no.elg.infiniteBootleg.util.Tuple;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.generator.simplex.PerlinNoise;

import java.util.ArrayList;
import java.util.List;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_HEIGHT;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_WIDTH;

/**
 * @author Elg
 */
public enum Biome {

    PLAINS(0.1, 0.9, 1, 64, 0.009, Material.STONE, new Tuple<>(Material.GRASS, 1), new Tuple<>(Material.DIRT, 10)),
    ANCIENT_MOUNTAINS(0.6, 0.9, 1, 256, 0.01, Material.STONE, new Tuple<>(Material.BRICK, 32)),
    ;

    public final double y;
    public final double z;
    public final double exponent;
    public final double amplitude;
    public final double frequency;
    public final Material filler;
    public final Material[] topSoil;

    Biome(double y, double z, double exponent, double amplitude, double frequency, Material filler,
          Tuple<Material, Integer>... topSoil) {
        this.y = y;
        this.z = z;
        this.exponent = exponent;
        this.amplitude = amplitude;
        this.frequency = frequency;
        this.filler = filler;
        List<Material> mats = new ArrayList<>();
        for (Tuple<Material, Integer> tuple : topSoil) {
            for (int i = 0; i < tuple.value; i++) {
                mats.add(tuple.key);
            }
        }
        this.topSoil = mats.toArray(new Material[0]);
    }

    public double heightAt(PerlinNoise noise, int chunkX, int localX) {
        return heightAt(noise, chunkX, localX, y, z, amplitude, frequency);
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

    public static double heightAt(PerlinNoise noise, int chunkX, int localX, double y, double z, double amplitude,
                                  double frequency) {
        int lx = localX + CHUNK_WIDTH * chunkX;
        return noise.octaveNoise(lx * frequency, y * frequency, z * frequency, 6, 0.5) * amplitude;
//        return ImprovedNoise.noise(localX + CHUNK_WIDTH * chunkPos.x, y, z, amplitude, frequency);
//        double nx = ;

//        double e = (weights[0] * + weights[1] * noise(noise, 2 * nx, 2 * ny) +
//                    weights[2] * noise(noise, 4 * nx, 4 * ny) + weights[3] * noise(noise, 8 * nx, 8 * ny) +
//                    weights[4] * noise(noise, 16 * nx, 16 * ny) + weights[5] * noise(noise, 32 * nx, 32 * ny));
//        e /= (weights[0] + weights[1] + weights[2] + weights[3] + weights[4] + weights[5]);
//        return ImprovedNoise.noise(localX + CHUNK_WIDTH * chunkPos.x, y, z, amplitude, frequency);
//        double e = noise
//            .getOctNoise((localX + CHUNK_WIDTH * chunkPos.x), (int) (weights[0] * weights[1]), (int) (weights[2] * weights[3]));
//        e = Math.pow(e, exponent);
//        System.out.println("e = " + e);
//        return (int) (e * intensity);
    }

    public void fillUpTo(PerlinNoise noise, Chunk chunk, int localX, int localY, int height) {
        int chunkY = chunk.getLocation().y * CHUNK_HEIGHT;
        for (int dy = 0; dy < localY; dy++) {
            chunk.setBlock(localX, dy, materialAt(noise, height, dy + chunkY), false);
        }
    }
}
