package no.elg.infiniteBootleg.world;

import no.elg.infiniteBootleg.world.generator.WorldGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Elg
 */
public class World {

    private final WorldGenerator generator;
    private final long seed;
    private final Random random;
    private final Map<Integer, Chunk> chunks;


    /**
     * Generate a world with a random seed
     *
     * @param generator
     */
    public World(@NotNull WorldGenerator generator) {
        this(generator, new Random().nextLong());
    }

    public World(@NotNull WorldGenerator generator, long seed) {
        this.generator = generator;
        this.seed = seed;
        random = new Random(seed);
        chunks = new ConcurrentHashMap<>();
    }

    @NotNull
    public Chunk getChunk(int offset) {
        return chunks.computeIfAbsent(offset, os -> generator.generateChunk(this, random, os));
    }

    @NotNull
    public Chunk getChunk(Location location) {
        return getChunk(location.x % Chunk.CHUNK_WIDTH);
    }

    public long getSeed() {
        return seed;
    }
}
