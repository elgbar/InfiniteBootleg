package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * Generate chunks that are always air
 *
 * @author Elg
 */
public class EmptyChunkGenerator implements ChunkGenerator {

    @Override
    public @NotNull Chunk generate(@NotNull World world, @NotNull Location chunkPos) {
        return new Chunk(world, chunkPos);
    }
}
