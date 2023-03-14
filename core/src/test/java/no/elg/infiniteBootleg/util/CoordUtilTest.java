package no.elg.infiniteBootleg.util;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author Elg
 */
public class CoordUtilTest {

  @Test
  public void calculateOffset() {
    for (int i = 0; i < CHUNK_SIZE; i++) {
      assertEquals("i:" + i, i, CoordUtilKt.chunkOffset(i));
    }
    for (int i = -CHUNK_SIZE; i < 0; i++) {
      assertEquals("i:" + i, i + CHUNK_SIZE, CoordUtilKt.chunkOffset(i));
    }
    assertEquals(0, CoordUtilKt.chunkOffset(CHUNK_SIZE));
    assertEquals(1, CoordUtilKt.chunkOffset(CHUNK_SIZE + 1));
    assertEquals(CHUNK_SIZE - 2, CoordUtilKt.chunkOffset(-2));
    assertEquals(CHUNK_SIZE - 1, CoordUtilKt.chunkOffset(CHUNK_SIZE + CHUNK_SIZE - 1));
  }

  @Test
  public void isInsideChunk() {
    for (int x = 0; x < CHUNK_SIZE; x++) {
      for (int y = 0; y < CHUNK_SIZE; y++) {
        assertTrue(CoordUtilKt.isInsideChunk(x, y));
      }
    }

    assertFalse(CoordUtilKt.isInsideChunk(-1, -1));
    assertFalse(CoordUtilKt.isInsideChunk(-1, 0));
    assertFalse(CoordUtilKt.isInsideChunk(0, -1));
    assertFalse(CoordUtilKt.isInsideChunk(CHUNK_SIZE, CHUNK_SIZE));
    assertFalse(CoordUtilKt.isInsideChunk(CHUNK_SIZE, 0));
    assertFalse(CoordUtilKt.isInsideChunk(0, CHUNK_SIZE));
  }

  @Test
  public void compactShort() {
    for (short x = -10; x <= 10; x++) {
      for (short y = -10; y <= 10; y++) {
        int compact = CoordUtilKt.compactShort(x, y);
        assertEquals(x, CoordUtilKt.decompactShortA(compact));
        assertEquals(y, CoordUtilKt.decompactShortB(compact));
      }
    }
  }
}
