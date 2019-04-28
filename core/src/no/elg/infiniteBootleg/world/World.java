package no.elg.infiniteBootleg.world;

import no.elg.infiniteBootleg.world.generator.WorldGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Elg
 */
public class World {

    private final WorldGenerator generator;
    private final Map<Integer, Chunk> chunks;

    public World(@NotNull WorldGenerator generator) {

        this.generator = generator;
        chunks = new ConcurrentHashMap<>();
    }

    @NotNull
    public Chunk getChunk(int offset) {
        return chunks.computeIfAbsent(offset, os -> generator.generateChunk(this, os));
    }
}
