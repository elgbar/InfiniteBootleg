package no.elg.infiniteBootleg.world.subgrid;

import com.badlogic.gdx.utils.ObjectSet;
import no.elg.infiniteBootleg.TestGraphic;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.subgrid.enitites.GenericEntity;
import no.elg.infiniteBootleg.world.world.World;
import org.junit.Assert;
import org.junit.Test;

public class EntityTest extends TestGraphic {

  @Test
  public void touchingBlockOneOne() {
    Entity ent = new GenericEntity(world, 0, 0, 1, 1);
    ObjectSet<Block> expectedBlocks = new ObjectSet<>();
    expectedBlocks.addAll( //
        world.getRawBlock(-1, -1, true), //
        world.getRawBlock(-1, 0, true), //
        world.getRawBlock(0, -1, true), //
        world.getRawBlock(0, 0, true));
    ObjectSet<Block> actualBlocks = ent.touchingBlocks(true);
    Assert.assertEquals(expectedBlocks, actualBlocks);
  }

  @Test
  public void touchingBlockMiddleOneOne() {
    Entity ent = new GenericEntity(world, 0.5f, 0.5f, 1, 1);
    World world = ent.getWorld();

    ObjectSet<Block> expectedBlocks = new ObjectSet<>();
    expectedBlocks.addAll( //
        world.getRawBlock(0, 0, true));
    ObjectSet<Block> actualBlocks = ent.touchingBlocks(true);
    Assert.assertEquals(expectedBlocks, actualBlocks);
  }

  @Test
  public void touchingBlockOneTwo() {
    Entity ent = new GenericEntity(world, 0, 0, 1, 2);
    ObjectSet<Block> expectedBlocks = new ObjectSet<>();
    expectedBlocks.addAll( //
        world.getRawBlock(-1, -1, true), //
        world.getRawBlock(-1, 0, true), //
        world.getRawBlock(0, -1, true), //
        world.getRawBlock(0, 0, true));
    ObjectSet<Block> actualBlocks = ent.touchingBlocks(true);
    Assert.assertEquals(expectedBlocks, actualBlocks);
  }

  @Test
  public void touchingBlockTwoOne() {
    Entity ent = new GenericEntity(world, 0, 0, 2, 1);
    ObjectSet<Block> expectedBlocks = new ObjectSet<>();
    expectedBlocks.addAll( //
        world.getRawBlock(-1, -1, true), //
        world.getRawBlock(-1, 0, true), //
        world.getRawBlock(0, -1, true), //
        world.getRawBlock(0, 0, true));
    ObjectSet<Block> actualBlocks = ent.touchingBlocks(true);
    Assert.assertEquals(expectedBlocks, actualBlocks);
  }

  @Test
  public void touchingBlockTwoOneNeg() {
    Entity ent = new GenericEntity(world, -5, -5, 2, 1);
    ObjectSet<Block> expectedBlocks = new ObjectSet<>();
    expectedBlocks.addAll( //
        world.getRawBlock(-6, -6, true), //
        world.getRawBlock(-6, -5, true), //
        world.getRawBlock(-5, -6, true), //
        world.getRawBlock(-5, -5, true));
    ObjectSet<Block> actualBlocks = ent.touchingBlocks(true);
    Assert.assertEquals(expectedBlocks, actualBlocks);
  }

  @Test
  public void touchingBlockTwoTwoMiddleNeg() {
    Entity ent = new GenericEntity(world, -5.5f, -5.5f, 2, 2);
    ObjectSet<Block> expectedBlocks = new ObjectSet<>();
    expectedBlocks.addAll( //
        world.getRawBlock(-7, -7, true), //
        world.getRawBlock(-7, -6, true), //
        world.getRawBlock(-7, -5, true), //
        world.getRawBlock(-6, -7, true), //
        world.getRawBlock(-6, -6, true), //
        world.getRawBlock(-6, -5, true), //
        world.getRawBlock(-5, -7, true), //
        world.getRawBlock(-5, -6, true), //
        world.getRawBlock(-5, -5, true));
    ObjectSet<Block> actualBlocks = ent.touchingBlocks(true);
    Assert.assertEquals(expectedBlocks, actualBlocks);
  }

  @Test
  public void touchingBlockDiff() {
    Entity ent = new GenericEntity(world, 2f, -100f, 3, 2);
    ObjectSet<Block> expectedBlocks = new ObjectSet<>();
    expectedBlocks.addAll( //
        world.getRawBlock(0, -101, true), //
        world.getRawBlock(0, -100, true), //
        world.getRawBlock(1, -101, true), //
        world.getRawBlock(1, -100, true), //
        world.getRawBlock(2, -101, true), //
        world.getRawBlock(2, -100, true), //
        world.getRawBlock(3, -101, true), //
        world.getRawBlock(3, -100, true));
    ObjectSet<Block> actualBlocks = ent.touchingBlocks(true);
    Assert.assertEquals(expectedBlocks, actualBlocks);
  }

  @Test
  public void touchingBlockDiffMiddle() {
    Entity ent = new GenericEntity(world, 2f, -100.5f, 3, 2);
    ObjectSet<Block> expectedBlocks = new ObjectSet<>();
    expectedBlocks.addAll( //
        world.getRawBlock(0, -102, true), //
        world.getRawBlock(0, -101, true), //
        world.getRawBlock(0, -100, true), //
        world.getRawBlock(1, -102, true), //
        world.getRawBlock(1, -101, true), //
        world.getRawBlock(1, -100, true), //
        world.getRawBlock(2, -102, true), //
        world.getRawBlock(2, -101, true), //
        world.getRawBlock(2, -100, true), //
        world.getRawBlock(3, -102, true), //
        world.getRawBlock(3, -101, true), //
        world.getRawBlock(3, -100, true));
    ObjectSet<Block> actualBlocks = ent.touchingBlocks(true);
    Assert.assertEquals(expectedBlocks, actualBlocks);
  }

  @Test
  public void touchingEntitiesNone() {
    Entity ent = new GenericEntity(world, 0, 0);
    Assert.assertTrue(ent.touchingEntities().isEmpty());
  }

  @Test
  public void touchingEntitiesSameLoc() {
    Entity ent = new GenericEntity(world, 0, 0);

    Entity ent2 = new GenericEntity(world, 1.1f, 1.1f);
    ent2.teleport(0, 0, false);

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
    Entity ent2 = new GenericEntity(world, 1.1f, 1.1f);
    ent2.teleport(0.1f, 0, false);
    Assert.assertEquals(1, ent.touchingEntities().size);
    Assert.assertEquals(1, ent2.touchingEntities().size);
    Assert.assertTrue(ent.touchingEntities().contains(ent2));
    Assert.assertTrue(ent2.touchingEntities().contains(ent));
  }

  @Test
  public void touchingEntitiesOverlapY() {
    Entity ent = new GenericEntity(world, 0, 0);
    Entity ent2 = new GenericEntity(world, 1.1f, 1.1f);
    ent2.teleport(0, 0.1f, false);
    Assert.assertEquals(1, ent.touchingEntities().size);
    Assert.assertEquals(1, ent2.touchingEntities().size);
    Assert.assertTrue(ent.touchingEntities().contains(ent2));
    Assert.assertTrue(ent2.touchingEntities().contains(ent));
  }

  @Test
  public void touchingEntitiesOverlap() {
    Entity ent = new GenericEntity(world, 0, 0, 2, 2);
    Entity ent2 = new GenericEntity(world, 1.1f, 1.1f);
    ent2.teleport(0, 0.1f, false);
    Assert.assertEquals(1, ent.touchingEntities().size);
    Assert.assertEquals(1, ent2.touchingEntities().size);
    Assert.assertTrue(ent.touchingEntities().contains(ent2));
    Assert.assertTrue(ent2.touchingEntities().contains(ent));
  }

  //    @Test
  //    public void touchingEntitiesOverlapNoCollision() {
  //        Entity ent = new GenericEntity(world, 0, 0, 2, 2, World.LIGHT_FILTER);
  //        Entity ent2 = new GenericEntity(world, 1.1f, 1.1f);
  //        ent2.teleport(0, 0.5f, false);
  //        Assert.assertTrue(ent.touchingEntities().isEmpty());
  //        Assert.assertTrue(ent2.touchingEntities().isEmpty());
  //    }

  @Test
  public void touchingEntitiesOverlapCollideSpawnValid() {
    Entity ent = new GenericEntity(world, 0, 0);
    Entity ent2 = new GenericEntity(world, 0, 0);
    Assert.assertTrue(ent2.touchingEntities().isEmpty());
    Assert.assertTrue(ent.touchingEntities().isEmpty());
  }
}
