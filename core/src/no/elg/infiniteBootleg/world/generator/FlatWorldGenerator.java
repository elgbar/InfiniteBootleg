package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.World;

/**
 * @author Elg
 */
public class FlatWorldGenerator implements WorldGenerator {

    @Override
    public Chunk generateChunk(World world, int offset) {
        Chunk chunk = new Chunk(world, offset);
//        chunk.

        return chunk;
    }
}
