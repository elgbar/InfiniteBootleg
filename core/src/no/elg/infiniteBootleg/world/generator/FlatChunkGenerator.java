package no.elg.infiniteBootleg.world.generator;

import com.google.common.base.Preconditions;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
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
public class FlatChunkGenerator implements ChunkGenerator {

    private Material[] layers;

    /**
     * Default flat world with stone from 0 to 31 and air as the rest
     */
    public FlatChunkGenerator() {
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

    public FlatChunkGenerator(@NotNull Material[] layers) {
        Preconditions.checkArgument(layers.length == Chunk.CHUNK_HEIGHT);
        //noinspection ConstantConditions
        Preconditions.checkArgument(Arrays.stream(layers).allMatch(Objects::nonNull));
        this.layers = layers;
    }

    @NotNull
    @Override
    public Chunk generate(@Nullable World world, @NotNull Location chunkPos, @NotNull Random random) {
        Chunk chunk = new Chunk(world, chunkPos);
        if (chunkPos.y < 0 || random.nextBoolean()) {
            for (int x = 0; x < Chunk.CHUNK_WIDTH; x++) {
                for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                    if (random.nextBoolean()) {
                        chunk.setBlock(x, y, Material.STONE);
                    }
                }
            }
        }
        return chunk;
    }
}
