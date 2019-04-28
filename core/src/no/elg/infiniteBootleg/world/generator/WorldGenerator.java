package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * @author Elg
 */
public interface WorldGenerator {

    /**
     * @param world
     *     The world this chunk exists in
     * @param random
     *     The random generator
     * @param offset
     *     The world offset
     *
     * @return A chunk at the given offset in the given world
     */
    @NotNull Chunk generateChunk(@Nullable World world, @NotNull Random random, int offset);
}
