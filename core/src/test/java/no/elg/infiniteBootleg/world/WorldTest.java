package no.elg.infiniteBootleg.world;

import no.elg.infiniteBootleg.TestGraphic;
import no.elg.infiniteBootleg.world.generator.EmptyChunkGenerator;
import org.junit.Before;
import org.junit.Test;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_HEIGHT;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_WIDTH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Elg
 */
public class WorldTest extends TestGraphic {

    private World world;
    private Location loc;

    @Before
    public void setUp() {
        loc = new Location(0, 0);
        world = new World(new EmptyChunkGenerator());
    }

    @Test
    public void canGenerateChunks() {
        Chunk chunk = world.getChunkFromWorld(loc);
        assertNotNull(chunk);
        assertEquals(chunk, world.getChunkFromWorld(loc));
    }

    @Test
    public void getCorrectChunkFromWorldCoords() {
        Chunk originChunk = world.getChunk(loc);
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            Chunk chunk = world.getChunkFromWorld(x, 0);
            assertEquals(originChunk, chunk);
        }
        assertEquals(world.getChunk(-1, 0), world.getChunkFromWorld(-2, 0));
        assertEquals(world.getChunk(-2, 0), world.getChunkFromWorld(-CHUNK_WIDTH - 1, 0));
        assertEquals(world.getChunk(-1, 0), world.getChunkFromWorld(-1, 0));
        assertEquals(world.getChunk(0, -1), world.getChunkFromWorld(0, -CHUNK_HEIGHT + 1));
        assertEquals(world.getChunk(1, 0), world.getChunkFromWorld(CHUNK_WIDTH, 0));
        assertEquals(world.getChunk(0, 0), world.getChunkFromWorld(CHUNK_WIDTH - 1, 0));
        assertEquals(world.getChunk(1, 0), world.getChunkFromWorld(CHUNK_WIDTH + 1, 0));
        assertEquals(world.getChunk(2, 0), world.getChunkFromWorld(CHUNK_WIDTH * 2, 0));
    }

    @Test
    public void setCorrectBlockFromOrigin() {
        world.setBlock(0, 0, Material.STONE, true);
        assertEquals(Material.STONE, world.getChunk(0, 0).getBlock(0, 0).getMaterial());
    }

    @Test
    public void setCorrectBlockFromWorldCoords() {
        world.setBlock(CHUNK_WIDTH + 1, 3 * CHUNK_HEIGHT + 9, Material.STONE, true);
        assertEquals(Material.STONE, world.getChunk(1, 3).getBlock(1, 9).getMaterial());
    }

    @Test
    public void setCorrectBlockFromWorldCoordsNeg() {
        world.setBlock(-CHUNK_WIDTH + 1, -3 * CHUNK_HEIGHT + 9, Material.STONE, true);
        assertEquals(Material.STONE, world.getChunk(-1, -3).getBlock(1, 9).getMaterial());
    }

    @Test
    public void getCorrectBlockFromOrigin() {
        world.getChunk(0, 0).setBlock(0, 0, Material.STONE, false);
        assertEquals(Material.STONE, world.getBlock(0, 0).getMaterial());
    }

    @Test
    public void getCorrectBlockFromWorldCoords() {
        world.getChunk(-2, 5).setBlock(2, 11, Material.STONE, false);
        assertEquals(Material.STONE, world.getBlock(-2 * CHUNK_WIDTH + 2, 5 * CHUNK_HEIGHT + 11).getMaterial());
    }
}
