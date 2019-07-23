package no.elg.infiniteBootleg.world.subgrid;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.TestGraphic;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.World;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EntityTest extends TestGraphic {

    private World world;

    @Before
    public void before() {
        world = Main.inst().getWorld();
    }

    @Test
    public void touchingBlockOneOne() {
        Entity ent = new EntityImpl(0, 0, 1, 1);
        Block[] expectedBlocks = { //
            world.getBlock(0, 0), //
        };
        Block[] actualBlocks = ent.touchingBlock().toArray();
        Assert.assertArrayEquals(expectedBlocks, actualBlocks);
    }

    @Test
    public void touchingBlockOneTwo() {
        Entity ent = new EntityImpl(0, 0, 1, 2);
        Block[] expectedBlocks = { //
            world.getBlock(0, 0), //
            world.getBlock(0, -1)};
        Block[] actualBlocks = ent.touchingBlock().toArray();
        Assert.assertArrayEquals(expectedBlocks, actualBlocks);
    }

    @Test
    public void touchingBlockTwoOne() {
        Entity ent = new EntityImpl(0, 0, 2, 1);
        Array<Block> blocks = new Array<>(Block.class);
        blocks.addAll(world.getBlock(0, 0), world.getBlock(1, 0));
        Assert.assertArrayEquals(blocks.toArray(), ent.touchingBlock().toArray());
    }

    @Test
    public void touchingBlockTwoOneNeg() {
        Entity ent = new EntityImpl(-5, -5, 2, 1);
        Block[] expectedBlocks = { //
            world.getBlock(-5, -5), //
            world.getBlock(-4, -5)};
        Block[] actualBlocks = ent.touchingBlock().toArray();
        Assert.assertArrayEquals(expectedBlocks, actualBlocks);
    }

    @Test
    public void touchingBlockTwoTwoMiddleNeg() {
        Entity ent = new EntityImpl(-5.5f, -5.5f, 2, 2);
        Block[] expectedBlocks = { //
            world.getBlock(-6, -6), //
            world.getBlock(-6, -7), //
            world.getBlock(-6, -8), //
            world.getBlock(-5, -6), //
            world.getBlock(-5, -7), //
            world.getBlock(-5, -8), //
            world.getBlock(-4, -6), //
            world.getBlock(-4, -7), //
            world.getBlock(-4, -8) //
        };
        Block[] actualBlocks = ent.touchingBlock().toArray();
        Assert.assertArrayEquals(expectedBlocks, actualBlocks);
    }

    @Test
    public void touchingBlockMiddleOneOne() {
        Entity ent = new EntityImpl(0.5f, 0.5f, 1, 1);
        World world = ent.getWorld();


        Block[] expectedBlocks = { //
            world.getBlock(0, 0), //
            world.getBlock(0, -1), //
            world.getBlock(1, 0), //
            world.getBlock(1, -1)};
        Block[] actualBlocks = ent.touchingBlock().toArray();
        Assert.assertArrayEquals(expectedBlocks, actualBlocks);
    }

    @Test
    public void touchingBlockDiff() {
        Entity ent = new EntityImpl(2f, -100f, 3, 2);
        Block[] expectedBlocks = { //
            world.getBlock(2, -100), //
            world.getBlock(2, -101), //
            world.getBlock(3, -100), //
            world.getBlock(3, -101), //
            world.getBlock(4, -100), //
            world.getBlock(4, -101), //
        };
        Block[] actualBlocks = ent.touchingBlock().toArray();
        Assert.assertArrayEquals(expectedBlocks, actualBlocks);
    }

    @Test
    public void touchingBlockDiffMiddle() {
        Entity ent = new EntityImpl(2f, -100.5f, 3, 2);
        Block[] expectedBlocks = { //
            world.getBlock(2, -101), //
            world.getBlock(2, -102), //
            world.getBlock(2, -103), //
            world.getBlock(3, -101), //
            world.getBlock(3, -102), //
            world.getBlock(3, -103), //
            world.getBlock(4, -101), //
            world.getBlock(4, -102), //
            world.getBlock(4, -103) //
        };
        Block[] actualBlocks = ent.touchingBlock().toArray();
        Assert.assertArrayEquals(expectedBlocks, actualBlocks);
    }

    private static class EntityImpl extends Entity {

        private final int width;
        private final int height;

        public EntityImpl(float worldX, float worldY, int width, int height) {
            super(Main.inst().getWorld(), worldX, worldY);
            this.width = width;
            this.height = height;
        }

        @Override
        public TextureRegion getTextureRegion() {
            return null;
        }

        @Override
        public int getWidth() {
            return width * Block.BLOCK_SIZE;
        }

        @Override
        public int getHeight() {
            return height * Block.BLOCK_SIZE;
        }
    }

}
