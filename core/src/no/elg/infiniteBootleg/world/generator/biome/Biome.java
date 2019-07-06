package no.elg.infiniteBootleg.world.generator.biome;

import com.badlogic.gdx.utils.Array;
import no.elg.infiniteBootleg.util.Tuple;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator;
import no.elg.infiniteBootleg.world.generator.noise.PerlinNoise;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public enum Biome {


    PLAINS(0.1, 0.9, 1, 64, 0.009, 0, Material.STONE, Material.GRASS, new Tuple<>(Material.DIRT, 10)),
    MOUNTAINS(100, 0.9, 1, 356, 0.005, 25, Material.STONE, Material.GRASS, new Tuple<>(Material.DIRT, 6)),
    DESERT(0.1, 0.9, 0.9, 32, 0.005, 0, Material.STONE, Material.SAND, new Tuple<>(Material.SAND, 12));

    public static final int INTERPOLATION_RADIUS = 25;

    private final double y;
    private final double z;
    private final double exponent;
    private final double amplitude;
    private final double frequency;
    private final int offset;
    private final Material filler;
    private final Material topmostBlock;
    private final Material[] topBlocks;

    @SafeVarargs
    Biome(double y, double z, double exponent, double amplitude, double frequency, int offset, @NotNull Material filler,
          @NotNull Material topmostBlock, @NotNull Tuple<Material, Integer>... topBlocks) {
        this.y = y;
        this.z = z;
        this.exponent = exponent;
        this.amplitude = amplitude;
        this.frequency = frequency;
        this.offset = offset;
        this.filler = filler;
        this.topmostBlock = topmostBlock;

        Array<Material> mats = new Array<>(true, 16, Material.class);
        for (Tuple<Material, Integer> tuple : topBlocks) {
            mats.ensureCapacity(tuple.value);
            for (int i = 0; i < tuple.value; i++) {
                mats.add(tuple.key);
            }
        }
        this.topBlocks = mats.toArray();
    }

    public static double rawHeightAt(@NotNull PerlinNoise noise, int worldX, double y, double z, double amplitude,
                                     double frequency, int offset) {
        return noise.octaveNoise(worldX * frequency, y * frequency, z * frequency, 6, 0.5) * amplitude + offset;
    }

    public Material materialAt(@NotNull PerlinNoise noise, int height, int worldX, int worldY) {
        int delta = height - worldY - 1;
        if (delta == 0) { return topmostBlock; }

        delta += (int) Math.abs(Math.floor(rawHeightAt(noise, worldX, y, z, 10, 0.05, 0)));

        if (delta >= topBlocks.length) { return filler; }
        return topBlocks[delta];
    }

    public int heightAt(@NotNull PerlinChunkGenerator pcg, int worldX) {
        int y = 0;
        for (int dx = -INTERPOLATION_RADIUS; dx <= INTERPOLATION_RADIUS; dx++) {
            if (dx != 0) {
                Biome biome = pcg.getBiome(worldX + dx);
                //NOTE it is not a bug that the worldX does not have dx added to it
                //well it was, but it looks better (ie more random) when the dx is not here
                y += biome.rawHeightAt(pcg.getNoise(), worldX);
            }
            else {
                y += rawHeightAt(pcg.getNoise(), worldX);
            }
        }
        return Math.abs((int) Math.floor(y / (INTERPOLATION_RADIUS * 2 + 1)));
    }

    public double rawHeightAt(@NotNull PerlinNoise noise, int worldX) {
        return rawHeightAt(noise, worldX, y, z, amplitude, frequency, offset);
    }

    public void fillUpTo(@NotNull PerlinNoise noise, @NotNull Chunk chunk, int localX, int localY, int height) {
        for (int dy = 0; dy < localY; dy++) {
            chunk.setBlock(localX, dy, materialAt(noise, height, chunk.getWorldX() + localX, chunk.getWorldY() + dy), false);
        }
    }
}
