package no.elg.infiniteBootleg.world.subgrid;

import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.TestGraphic;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.subgrid.enitites.GenericEntity;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EntityTest extends TestGraphic {

    private World world;

    @Before
    public void before() {
        world = Main.inst().getWorld();
    }

    @After
    public void after() {
        world.getEntities().clear();
    }

    @Test
    public void touchingBlockOneOne() {
        Entity ent = new GenericEntity(world, 0, 0, 1, 1);
        Block[] expectedBlocks = { //
            world.getBlock(-1, -1), //
            world.getBlock(-1, 0), //
            world.getBlock(0, -1), //
            world.getBlock(0, 0), //
        };
        Block[] actualBlocks = ent.touchingBlocks().toArray();
        Assert.assertArrayEquals(expectedBlocks, actualBlocks);
    }


    @Test
    public void touchingBlockMiddleOneOne() {
        Entity ent = new GenericEntity(world, 0.5f, 0.5f, 1, 1);
        World world = ent.getWorld();

        Block[] expectedBlocks = { //
            world.getBlock(0, 0), //
        };
        Block[] actualBlocks = ent.touchingBlocks().toArray();
        Assert.assertArrayEquals(expectedBlocks, actualBlocks);
    }

    @Test
    public void touchingBlockOneTwo() {
        Entity ent = new GenericEntity(world, 0, 0, 1, 2);
        Block[] expectedBlocks = { //
            world.getBlock(-1, -1), //
            world.getBlock(-1, 0), //
            world.getBlock(0, -1), //
            world.getBlock(0, 0), //
        };
        Block[] actualBlocks = ent.touchingBlocks().toArray();
        Assert.assertArrayEquals(expectedBlocks, actualBlocks);
    }

    @Test
    public void touchingBlockTwoOne() {
        Entity ent = new GenericEntity(world, 0, 0, 2, 1);
        Block[] expectedBlocks = { //
            world.getBlock(-1, -1), //
            world.getBlock(-1, 0), //
            world.getBlock(0, -1), //
            world.getBlock(0, 0), //
        };
        Block[] actualBlocks = ent.touchingBlocks().toArray();
        Assert.assertArrayEquals(expectedBlocks, actualBlocks);
    }

    @Test
    public void touchingBlockTwoOneNeg() {
        Entity ent = new GenericEntity(world, -5, -5, 2, 1);
        Block[] expectedBlocks = { //
            world.getBlock(-6, -6), //
            world.getBlock(-6, -5), //
            world.getBlock(-5, -6), //
            world.getBlock(-5, -5), //
        };
        Block[] actualBlocks = ent.touchingBlocks().toArray();
        Assert.assertArrayEquals(expectedBlocks, actualBlocks);
    }

    @Test
    public void touchingBlockTwoTwoMiddleNeg() {
        Entity ent = new GenericEntity(world, -5.5f, -5.5f, 2, 2);
        Block[] expectedBlocks = { //
            world.getBlock(-7, -7), //
            world.getBlock(-7, -6), //
            world.getBlock(-7, -5), //
            world.getBlock(-6, -7), //
            world.getBlock(-6, -6), //
            world.getBlock(-6, -5), //
            world.getBlock(-5, -7), //
            world.getBlock(-5, -6), //
            world.getBlock(-5, -5) //
        };
        Block[] actualBlocks = ent.touchingBlocks().toArray();
        Assert.assertArrayEquals(expectedBlocks, actualBlocks);
    }

    @Test
    public void touchingBlockDiff() {
        Entity ent = new GenericEntity(world, 2f, -100f, 3, 2);
        Block[] expectedBlocks = { //
            world.getBlock(0, -101), //
            world.getBlock(0, -100), //
            world.getBlock(1, -101), //
            world.getBlock(1, -100), //
            world.getBlock(2, -101), //
            world.getBlock(2, -100), //
            world.getBlock(3, -101), //
            world.getBlock(3, -100), //
        };
        Block[] actualBlocks = ent.touchingBlocks().toArray();
        Assert.assertArrayEquals(expectedBlocks, actualBlocks);
    }

    @Test
    public void touchingBlockDiffMiddle() {
        Entity ent = new GenericEntity(world, 2f, -100.5f, 3, 2);
        Block[] expectedBlocks = { //
            world.getBlock(0, -102), //
            world.getBlock(0, -101), //
            world.getBlock(0, -100), //
            world.getBlock(1, -102), //
            world.getBlock(1, -101), //
            world.getBlock(1, -100), //
            world.getBlock(2, -102), //
            world.getBlock(2, -101), //
            world.getBlock(2, -100), //
            world.getBlock(3, -102), //
            world.getBlock(3, -101), //
            world.getBlock(3, -100), //
        };
        Block[] actualBlocks = ent.touchingBlocks().toArray();
        Assert.assertArrayEquals(expectedBlocks, actualBlocks);
    }


    @Test
    public void touchingEntitiesNone() {
        Entity ent = new GenericEntity(world, 0, 0);
        Assert.assertTrue(ent.touchingEntities().isEmpty());
    }

    @Test
    public void touchingEntitiesSameLoc() {
        Entity ent = new GenericEntity(world, 0, 0);
        Entity ent2 = new GenericEntity(world, 0, 0);
        Assert.assertEquals(1, ent.touchingEntities().size);
        Assert.assertEquals(1, ent2.touchingEntities().size);
        Assert.assertTrue(ent.touchingEntities().contains(ent2));
        Assert.assertTrue(ent2.touchingEntities().contains(ent));
    }

    @Test
    public void touchingEntitiesDiffLoc() {
        Entity ent = new GenericEntity(world, 0, 0);
        Entity ent2 = new GenericEntity(world, 1, 0);
        Assert.assertTrue(ent.touchingEntities().isEmpty());
        Assert.assertTrue(ent2.touchingEntities().isEmpty());
    }

    @Test
    public void touchingEntitiesOverlapX() {
        Entity ent = new GenericEntity(world, 0, 0);
        Entity ent2 = new GenericEntity(world, 0.1f, 0);
        Assert.assertEquals(1, ent.touchingEntities().size);
        Assert.assertEquals(1, ent2.touchingEntities().size);
        Assert.assertTrue(ent.touchingEntities().contains(ent2));
        Assert.assertTrue(ent2.touchingEntities().contains(ent));
    }

    @Test
    public void touchingEntitiesOverlapY() {
        Entity ent = new GenericEntity(world, 0, 0);
        Entity ent2 = new GenericEntity(world, 0f, 0.1f);
        Assert.assertEquals(1, ent.touchingEntities().size);
        Assert.assertEquals(1, ent2.touchingEntities().size);
        Assert.assertTrue(ent.touchingEntities().contains(ent2));
        Assert.assertTrue(ent2.touchingEntities().contains(ent));
    }

    @Test
    public void touchingEntitiesOverlap() {
        Entity ent = new GenericEntity(world, 0, 0, 2, 2);
        Entity ent2 = new GenericEntity(world, 0f, 0.5f);
        Assert.assertEquals(1, ent.touchingEntities().size);
        Assert.assertEquals(1, ent2.touchingEntities().size);
        Assert.assertTrue(ent.touchingEntities().contains(ent2));
        Assert.assertTrue(ent2.touchingEntities().contains(ent));
    }
}
