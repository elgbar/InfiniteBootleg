package no.elg.infiniteBootleg.world;

import no.elg.infiniteBootleg.world.generator.FlatWorldGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_HEIGHT;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_WIDTH;

/**
 * @author Elg
 */
public class ChunkTest {

    private World world;

    @Before
    public void setUp() throws Exception {

        world = new World(new FlatWorldGenerator());
    }

    @Test
    public void invalidBlocksWidth() {
        Assert.assertEquals(CHUNK_WIDTH, new Chunk(world, 0).getBlocks().length);
    }

    @Test
    public void invalidBlocksHeight() {
        Assert.assertEquals(CHUNK_HEIGHT, new Chunk(world, 0).getBlocks()[0].length);
    }

    @Test
    public void getLocalBlocks() {
        Chunk chunk = new Chunk(world, 0);
        Block[][] blocks = chunk.getBlocks();


    }
}
