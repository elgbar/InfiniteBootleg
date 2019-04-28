package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.World;

/**
 * @author Elg
 */
public interface WorldGenerator {

    Chunk generateChunk(World world, int offset);
}
