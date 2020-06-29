package no.elg.infiniteBootleg.world;

import no.elg.infiniteBootleg.TestGraphic;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Elg
 */

public class BlockTest extends TestGraphic {

    @Test
    public void correctBlockType() {
        for (Material mat : Material.values()) {
            if (mat.isEntity()) { continue; }
            Chunk c = new Chunk(world, 0, 0);
            c.finishLoading();
            Block b = mat.createBlock(world, c, 0, 0);
            assertEquals(mat, b.getMaterial());
        }
    }

    @Test
    public void correctCoordinated() {
        assertEquals(-1, world.getBlock(-1, 0, false).getWorldX());
        assertEquals(-1, world.getBlock(-2, -1, false).getWorldY());
    }

    @Test
    public void getCorrectChunk() {
        Block block = world.getBlock(-1, -1, false);
        assertEquals(world.getChunkFromWorld(-1, -1), block.getChunk());
        assertEquals(world.getChunk(-1, -1), block.getChunk());
    }

    @Test
    public void getRelativeBlock() {
        Block b = world.getBlock(0, 0, false);
        Material mat = Material.BRICK;
        for (Direction dir : Direction.values()) {
            world.setBlock(dir.dx, dir.dy, mat);
            assertEquals(mat, b.getRelative(dir).getMaterial());
        }
        world.setBlock(0, 0, mat);

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                assertEquals(mat, world.getBlock(x, y, false).getMaterial());
            }
        }
    }
}
