package no.elg.infiniteBootleg.util;

import no.elg.infiniteBootleg.world.Location;
import org.junit.Test;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_HEIGHT;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_WIDTH;
import static org.junit.Assert.assertEquals;

/**
 * @author Elg
 */
public class CoordUtilTest {

    @Test
    public void worldToChunk() {
        assertEquals(new Location(1, 1), CoordUtil.worldToChunk(new Location(CHUNK_WIDTH, CHUNK_HEIGHT)));
        assertEquals(new Location(4, -1231), CoordUtil.worldToChunk(new Location(4 * CHUNK_WIDTH, -1231 * CHUNK_HEIGHT)));
        assertEquals(new Location(0, 0), CoordUtil.worldToChunk(new Location(CHUNK_WIDTH - 1, CHUNK_HEIGHT - 1)));
        assertEquals(new Location(0, 0), CoordUtil.worldToChunk(new Location(1, 1)));
    }

    @Test
    public void chunkToWorld() {
        assertEquals(new Location(CHUNK_WIDTH, CHUNK_HEIGHT), CoordUtil.chunkToWorld(new Location(1, 1)));
        assertEquals(new Location(2 * CHUNK_WIDTH, -2 * CHUNK_HEIGHT), CoordUtil.chunkToWorld(new Location(2, -2)));
        assertEquals(new Location(0, 0), CoordUtil.chunkToWorld(new Location(0, 0)));
        assertEquals(new Location(0, 0), CoordUtil.chunkToWorld(new Location(0, 0)));
    }
}
