package no.elg.infiniteBootleg.world;

import no.elg.infiniteBootleg.TestGraphic;
import no.elg.infiniteBootleg.world.generator.EmptyChunkGenerator;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Elg
 */
public class BlockTest extends TestGraphic {

    private World world;

    @Before
    public void setUp() {
        world = new World(new EmptyChunkGenerator());
    }

    @Test
    public void correctType() {
        for (Material mat : Material.values()) {
            Block b = mat.create(world, new Chunk(world, new Location(0, 0)), 0, 0);
            assertEquals(mat, b.getMaterial());
        }
    }

    @Test
    public void correctCoordinated() {
        assertEquals(world.getBlock(-1, -1).getWorldLoc(), new Location(-1, -1));
    }

    @Test
    public void getCorrectChunk() {
        Block block = world.getBlock(-1, -1);
        assertEquals(world.getChunkFromWorld(-1, -1), block.getChunk());
        assertEquals(world.getChunk(-1, -1), block.getChunk());
    }

    @Test
    public void getRelativeBlock() {
        Block b = world.getBlock(0, 0);
        Material mat = Material.BRICK;
        for (Direction dir : Direction.values()) {
            world.setBlock(dir.dx, dir.dy, mat, true);
            assertEquals(mat, b.getRelative(dir).getMaterial());
        }
        world.setBlock(0, 0, mat, true);

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                assertEquals(mat, world.getBlock(x, y).getMaterial());
            }
        }

    }
}
