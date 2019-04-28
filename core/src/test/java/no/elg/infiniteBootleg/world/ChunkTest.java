package no.elg.infiniteBootleg.world;

import no.elg.infiniteBootleg.TestGraphic;
import org.junit.Before;
import org.junit.Test;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_HEIGHT;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_WIDTH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Elg
 */
public class ChunkTest extends TestGraphic {

    private Chunk chunk;

    @Before
    public void setUp() {
        chunk = new Chunk(0, null);
    }

    @Test
    public void invalidBlocksWidth() {
        assertEquals(CHUNK_WIDTH, chunk.getBlocks().length);
    }

    @Test
    public void invalidBlocksHeight() {
        assertEquals(CHUNK_HEIGHT, chunk.getBlocks()[0].length);
    }

    @Test
    public void allInitialBlocksAir() {
        for (Block block : chunk) {
            assertEquals(Material.AIR, block.getMaterial());
        }
    }

    @Test
    public void getLocalBlocks() {
        Block[][] blocks = chunk.getBlocks();
        for (Block block : chunk) {
            assertNotNull(block);
            Location pos = block.getLocation();
            assertEquals(blocks[pos.x][pos.y], block);
        }
    }

    @Test
    public void correctSizeOfIterable() {
        assertEquals(CHUNK_WIDTH * CHUNK_HEIGHT, chunk.stream().count());
    }
}
