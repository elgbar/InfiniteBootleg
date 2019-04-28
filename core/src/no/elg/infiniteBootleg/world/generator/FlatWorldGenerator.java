package no.elg.infiniteBootleg.world.generator;

import com.google.common.base.Preconditions;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/**
 * @author Elg
 */
public class FlatWorldGenerator implements WorldGenerator {

    private Material[] layers;

    /**
     * Default flat world with stone from 0 to 31 and air as the rest
     */
    public FlatWorldGenerator() {
        layers = new Material[Chunk.CHUNK_HEIGHT];
        for (int y = 0, length = layers.length; y < length; y++) {
            if (y < 32) {
                layers[y] = Material.STONE;
            }
            else {
                layers[y] = Material.AIR;
            }
        }
    }

    public FlatWorldGenerator(@NotNull Material[] layers) {
        Preconditions.checkArgument(layers.length == Chunk.CHUNK_HEIGHT);
        //noinspection ConstantConditions
        Preconditions.checkArgument(Arrays.stream(layers).allMatch(Objects::nonNull));
        this.layers = layers;
    }

    @NotNull
    @Override
    public Chunk generateChunk(@Nullable World world, @NotNull Random random, int offset) {
        Chunk chunk = new Chunk(offset, world);
        for (int x = 0; x < Chunk.CHUNK_WIDTH; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {

            }
        }


        return chunk;
    }
}
