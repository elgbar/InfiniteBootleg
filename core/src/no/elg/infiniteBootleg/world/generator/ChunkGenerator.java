package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * @author Elg
 */
public interface ChunkGenerator {

    /**
     * @return A chunk at the given offset in the given world
     */
    @NotNull Chunk generate(@Nullable World world, @NotNull Location chunkPos, @NotNull Random random);
}
