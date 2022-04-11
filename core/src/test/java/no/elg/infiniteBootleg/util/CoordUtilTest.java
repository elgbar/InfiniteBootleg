package no.elg.infiniteBootleg.util;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import no.elg.infiniteBootleg.world.Location;
import org.junit.Test;

/**
 * @author Elg
 */
public class CoordUtilTest {

  @Test
  public void worldToChunk() {
    assertEquals(new Location(1, 1), CoordUtil.worldToChunk(new Location(CHUNK_SIZE, CHUNK_SIZE)));
    assertEquals(
        new Location(4, -1231),
        CoordUtil.worldToChunk(new Location(4 * CHUNK_SIZE, -1231 * CHUNK_SIZE)));
    assertEquals(
        new Location(0, 0), CoordUtil.worldToChunk(new Location(CHUNK_SIZE - 1, CHUNK_SIZE - 1)));
    assertEquals(new Location(0, 0), CoordUtil.worldToChunk(new Location(1, 1)));
    assertEquals(1, CoordUtil.worldToChunk(CHUNK_SIZE));
  }

  @Test
  public void chunkToWorld() {
    assertEquals(new Location(CHUNK_SIZE, CHUNK_SIZE), CoordUtil.chunkToWorld(new Location(1, 1)));
    assertEquals(
        new Location(2 * CHUNK_SIZE, -2 * CHUNK_SIZE), CoordUtil.chunkToWorld(new Location(2, -2)));
    assertEquals(new Location(0, 0), CoordUtil.chunkToWorld(new Location(0, 0)));
    assertEquals(0, CoordUtil.chunkToWorld(0));
    assertEquals(CHUNK_SIZE, CoordUtil.chunkToWorld(1));
    assertEquals(2 * CHUNK_SIZE, CoordUtil.chunkToWorld(2));

    assertEquals(11, CoordUtil.chunkToWorld(0, 11));
    assertEquals(11 + CHUNK_SIZE, CoordUtil.chunkToWorld(1, 11));
  }

  @Test
  public void calculateOffset() {
    for (int i = 0; i < CHUNK_SIZE; i++) {
      assertEquals("i:" + i, i, CoordUtil.chunkOffset(i));
    }
    for (int i = -CHUNK_SIZE; i < 0; i++) {
      assertEquals("i:" + i, i + CHUNK_SIZE, CoordUtil.chunkOffset(i));
    }
    assertEquals(0, CoordUtil.chunkOffset(CHUNK_SIZE));
    assertEquals(1, CoordUtil.chunkOffset(CHUNK_SIZE + 1));
    assertEquals(CHUNK_SIZE - 2, CoordUtil.chunkOffset(-2));
    assertEquals(CHUNK_SIZE - 1, CoordUtil.chunkOffset(CHUNK_SIZE + CHUNK_SIZE - 1));
  }

  @Test
  public void chunkToWorldOffset() {
    assertEquals(
        CoordUtil.chunkToWorld(new Location(1, 1)),
        CoordUtil.chunkToWorld(new Location(1, 1), 0, 0));
    assertEquals(
        CoordUtil.chunkToWorld(new Location(2, -2)),
        CoordUtil.chunkToWorld(new Location(2, -2), 0, 0));
    assertEquals(
        CoordUtil.chunkToWorld(new Location(0, 0)),
        CoordUtil.chunkToWorld(new Location(0, 0), 0, 0));
    assertEquals(new Location(3, 2), CoordUtil.chunkToWorld(new Location(0, 0), 3, 2));
    assertEquals(
        new Location(3 * CHUNK_SIZE + 3, -CHUNK_SIZE + 2),
        CoordUtil.chunkToWorld(new Location(3, -1), 3, 2));
  }

  @Test
  public void isInsideChunk() {
    for (int x = 0; x < CHUNK_SIZE; x++) {
      for (int y = 0; y < CHUNK_SIZE; y++) {
        assertTrue(CoordUtil.isInsideChunk(x, y));
      }
    }

    assertFalse(CoordUtil.isInsideChunk(-1, -1));
    assertFalse(CoordUtil.isInsideChunk(-1, 0));
    assertFalse(CoordUtil.isInsideChunk(0, -1));
    assertFalse(CoordUtil.isInsideChunk(CHUNK_SIZE, CHUNK_SIZE));
    assertFalse(CoordUtil.isInsideChunk(CHUNK_SIZE, 0));
    assertFalse(CoordUtil.isInsideChunk(0, CHUNK_SIZE));
  }

  @Test
  public void compactLocations() {
    for (int x = -10; x <= 10; x++) {
      for (int y = -10; y <= 10; y++) {
        long compact = CoordUtil.compactLoc(x, y);
        assertEquals(x, CoordUtil.decompactLocX(compact));
        assertEquals(y, CoordUtil.decompactLocY(compact));
        assertEquals(new Location(x, y), CoordUtil.decompactLoc(compact));
      }
    }
  }

  @Test
  public void compactShort() {
    for (short x = -10; x <= 10; x++) {
      for (short y = -10; y <= 10; y++) {
        int compact = CoordUtil.compactShort(x, y);
        assertEquals(x, CoordUtil.decompactShortA(compact));
        assertEquals(y, CoordUtil.decompactShortB(compact));
      }
    }
  }
}
