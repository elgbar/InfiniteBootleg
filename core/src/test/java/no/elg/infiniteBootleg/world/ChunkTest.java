package no.elg.infiniteBootleg.world;

import no.elg.infiniteBootleg.TestGraphic;
import no.elg.infiniteBootleg.world.generator.EmptyChunkGenerator;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_HEIGHT;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_WIDTH;
import static org.junit.Assert.*;

/**
 * @author Elg
 */
public class ChunkTest extends TestGraphic {

    private Chunk chunk;
    private Location loc;

    @Before
    public void setUp() {
        loc = new Location(0, 0);
        chunk = new Chunk(new World(new EmptyChunkGenerator()), loc);
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
    public void correctSizeOfIterable() {
        assertEquals(CHUNK_WIDTH * CHUNK_HEIGHT, chunk.stream().count());
    }

    @Test
    public void allBlocksIterated() {
        List<Block> itrBlocks = chunk.stream().collect(Collectors.toList());
        Set<Block> rawBlocks = new HashSet<>();
        for (Block[] blocks : chunk.getBlocks()) {
            rawBlocks.addAll(Arrays.asList(blocks));
        }

        assertTrue(itrBlocks.containsAll(rawBlocks));
        assertTrue(rawBlocks.containsAll(itrBlocks));
    }

    @Test
    public void checkAllAirModified() {
        chunk.setBlock(0, 0, Material.AIR, false);
        chunk.updateTextureNow();
        assertTrue(chunk.isAllAir());
        chunk.setBlock(0, 0, Material.STONE, false);
        chunk.updateTextureNow();
        assertFalse(chunk.isAllAir());
        chunk.setBlock(0, 0, Material.AIR, false);
        chunk.updateTextureNow();
        assertTrue(chunk.isAllAir());
    }

    @Test
    public void setAndGetCorrectBlock() {
        assertEquals(Material.AIR, chunk.getBlock(0, 0).getMaterial());
        chunk.setBlock(0, 0, Material.STONE, false);

        assertEquals(Material.STONE, chunk.getBlock(0, 0).getMaterial());
        assertEquals(Material.STONE, chunk.getBlock(0, 0).getMaterial());

        chunk.setBlock(0, 0, Material.AIR, false);
        assertEquals(Material.AIR, chunk.getBlock(0, 0).getMaterial());
    }

    @Test
    public void locationMatch() {
        chunk.setBlock(0, 0, Material.AIR, false);
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = 0; y < CHUNK_HEIGHT; y++) {
                Location loc = chunk.getBlock(x, y).getWorldLoc();
                assertEquals(x, loc.x);
                assertEquals(y, loc.y);
            }
        }
    }
}
