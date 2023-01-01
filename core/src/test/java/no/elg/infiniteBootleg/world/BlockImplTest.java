package no.elg.infiniteBootleg.world;

import static org.junit.Assert.assertEquals;

import no.elg.infiniteBootleg.TestGraphic;
import no.elg.infiniteBootleg.util.CoordUtil;
import org.junit.Test;

/**
 * @author Elg
 */
public class BlockImplTest extends TestGraphic {

  @Test
  public void correctBlockType() {
    for (Material mat : Material.values()) {
      if (mat.isEntity()) {
        continue;
      }
      ChunkImpl c = new ChunkImpl(world, 0, 0);
      c.finishLoading();
      Block b = mat.createBlock(world, c, 0, 0);
      assertEquals(mat, b.getMaterial());
    }
  }

  @Test
  public void correctCoordinated() {
    assertEquals(-1, world.getRawBlock(-1, 0, true).getWorldX());
    assertEquals(-1, world.getRawBlock(-2, -1, true).getWorldY());
  }

  @Test
  public void getCorrectChunk() {
    Block block = world.getRawBlock(-1, -1, true);
    assertEquals(world.getChunkFromWorld(-1, -1, true), block.getChunk());
    assertEquals(world.getChunk(CoordUtil.compactLoc(-1, -1)), block.getChunk());
  }

  @Test
  public void getRelativeBlock() {
    Block b = world.getRawBlock(0, 0, true);
    Material mat = Material.BRICK;
    for (Direction dir : Direction.values()) {
      world.setBlock(dir.dx, dir.dy, mat);
      assertEquals(mat, b.getRelative(dir).getMaterial());
    }
    world.setBlock(0, 0, mat);

    for (int x = -1; x <= 1; x++) {
      for (int y = -1; y <= 1; y++) {
        assertEquals(mat, world.getRawBlock(x, y, true).getMaterial());
      }
    }
  }
}
