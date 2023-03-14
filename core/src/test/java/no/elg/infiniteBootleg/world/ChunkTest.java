package no.elg.infiniteBootleg.world;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.elg.infiniteBootleg.TestGraphic;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Elg
 */
public class ChunkTest extends TestGraphic {

  private ChunkImpl chunk;

  @Before
  public void setUp() {
    chunk = new ChunkImpl(world, 0, 0);
    chunk.finishLoading();
  }

  @Test
  public void invalidBlocksWidth() {
    assertEquals(CHUNK_SIZE, chunk.blocks.length);
  }

  @Test
  public void invalidBlocksHeight() {
    assertEquals(CHUNK_SIZE, chunk.blocks[0].length);
  }

  @Test
  public void correctSizeOfIterable() {
    assertEquals(CHUNK_SIZE * CHUNK_SIZE, chunk.stream().count());
  }

  @Test
  public void allBlocksIterated() {
    List<Block> itrBlocks = chunk.stream().collect(Collectors.toList());
    Set<Block> rawBlocks = new HashSet<>();
    for (Block[] blocks : chunk.blocks) {
      rawBlocks.addAll(Arrays.asList(blocks));
    }

    assertTrue(itrBlocks.containsAll(rawBlocks));
    assertTrue(rawBlocks.containsAll(itrBlocks));
  }

  @Test
  public void checkAllAirModified() {
    chunk.setBlock(0, 0, Material.AIR, false);
    chunk.updateIfDirty();
    assertTrue(chunk.isAllAir());
    chunk.setBlock(0, 0, Material.STONE, false);
    chunk.updateIfDirty();
    assertFalse(chunk.isAllAir());
    chunk.setBlock(0, 0, Material.AIR, false);
    chunk.updateIfDirty();
    assertTrue(chunk.isAllAir());
  }

  @Test
  public void setAndGetCorrectBlock() {
    assertEquals(Material.AIR, chunk.getBlock(0, 0).getMaterial());
    chunk.setBlock(0, 0, Material.STONE, false);

    assertEquals(Material.STONE, chunk.getBlock(0, 0).getMaterial());
    assertEquals(Material.STONE, chunk.getBlock(0, 0).getMaterial());

    chunk.setBlock(0, 0, Material.AIR, false);
    assertEquals(Material.AIR, chunk.getBlock(0, 0).getMaterial());
  }

  @Test
  public void locationMatch() {
    chunk.setBlock(0, 0, Material.AIR, false);
    for (int x = 0; x < CHUNK_SIZE; x++) {
      for (int y = 0; y < CHUNK_SIZE; y++) {
        Block b = chunk.getBlock(x, y);
        assertEquals(x, b.getWorldX());
        assertEquals(y, b.getWorldY());
      }
    }
  }
}
