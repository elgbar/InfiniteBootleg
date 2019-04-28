package no.elg.infiniteBootleg.world;

import no.elg.infiniteBootleg.world.generator.FlatWorldGenerator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Elg
 */
public class WorldTest {

    @Test
    public void canGenerateChunks() {
        World world = new World(new FlatWorldGenerator());
        Chunk chunk = world.getChunk(0);
        assertNotNull(chunk);
        assertEquals(chunk, world.getChunk(0));
    }
}
