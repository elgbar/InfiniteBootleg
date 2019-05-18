package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Generate chunks that are always air
 *
 * @author Elg
 */
public class EmptyChunkGenerator implements ChunkGenerator {

    @Override
    public @NotNull Chunk generate(@Nullable World world, @NotNull Location chunkPos, @NotNull Random random) {
        return new Chunk(world, chunkPos);
    }
}
