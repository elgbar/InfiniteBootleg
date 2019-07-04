package no.elg.infiniteBootleg.util;

import no.elg.infiniteBootleg.world.Location;
import org.junit.Test;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;
import static org.junit.Assert.assertEquals;

/**
 * @author Elg
 */
public class CoordUtilTest {

    @Test
    public void worldToChunk() {
        assertEquals(new Location(1, 1), CoordUtil.worldToChunk(new Location(CHUNK_SIZE, CHUNK_SIZE)));
        assertEquals(new Location(4, -1231), CoordUtil.worldToChunk(new Location(4 * CHUNK_SIZE, -1231 * CHUNK_SIZE)));
        assertEquals(new Location(0, 0), CoordUtil.worldToChunk(new Location(CHUNK_SIZE - 1, CHUNK_SIZE - 1)));
        assertEquals(new Location(0, 0), CoordUtil.worldToChunk(new Location(1, 1)));
    }

    @Test
    public void chunkToWorld() {
        assertEquals(new Location(CHUNK_SIZE, CHUNK_SIZE), CoordUtil.chunkToWorld(new Location(1, 1)));
        assertEquals(new Location(2 * CHUNK_SIZE, -2 * CHUNK_SIZE), CoordUtil.chunkToWorld(new Location(2, -2)));
        assertEquals(new Location(0, 0), CoordUtil.chunkToWorld(new Location(0, 0)));
    }

    @Test
    public void calculateOffset() {
        for (int i = 0; i < CHUNK_SIZE; i++) {
            assertEquals(i, CoordUtil.calculateOffset(i));
        }
        assertEquals(0, CoordUtil.calculateOffset(CHUNK_SIZE));
        assertEquals(1, CoordUtil.calculateOffset(CHUNK_SIZE + 1));
        assertEquals(CHUNK_SIZE - 1, CoordUtil.calculateOffset(CHUNK_SIZE + CHUNK_SIZE - 1));
    }

    @Test
    public void chunkToWorldOffset() {
        assertEquals(CoordUtil.chunkToWorld(new Location(1, 1)), CoordUtil.chunkToWorld(new Location(1, 1), 0, 0));
        assertEquals(CoordUtil.chunkToWorld(new Location(2, -2)), CoordUtil.chunkToWorld(new Location(2, -2), 0, 0));
        assertEquals(CoordUtil.chunkToWorld(new Location(0, 0)), CoordUtil.chunkToWorld(new Location(0, 0), 0, 0));
        assertEquals(new Location(3, 2), CoordUtil.chunkToWorld(new Location(0, 0), 3, 2));
        assertEquals(new Location(3 * CHUNK_SIZE + 3, -CHUNK_SIZE + 2), CoordUtil.chunkToWorld(new Location(3, -1), 3, 2));
    }
}
