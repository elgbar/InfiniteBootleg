package no.elg.infiniteBootleg.world;

import box2dLight.DirectionalLight;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ObjectSet;
import no.elg.infiniteBootleg.TestGraphic;
import no.elg.infiniteBootleg.world.generator.EmptyChunkGenerator;
import no.elg.infiniteBootleg.world.render.WorldRender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Elg
 */
public class WorldTest extends TestGraphic {

    private World world;
    private Location loc;

    @Before
    public void setUp() {
        loc = new Location(0, 0);
        world = Mockito.spy(new World(new EmptyChunkGenerator()));

        WorldRender wr = mock(WorldRender.class);
        when(world.getRender()).thenReturn(wr);

        DirectionalLight dl = mock(DirectionalLight.class);
        when(wr.getSkylight()).thenReturn(dl);

        final Color color = new Color(Color.WHITE);

        when(dl.getColor()).thenReturn(color);

        Mockito.doAnswer(i -> color.set(i.getArgument(0))).when(dl).setColor(any(Color.class));
        Mockito.doAnswer(i -> color.set(i.getArgument(0), i.getArgument(1), i.getArgument(2), i.getArgument(3))).when(
            dl).setColor(any(Float.class), any(Float.class), any(Float.class), any(Float.class));
    }

    @Test
    public void canGenerateChunks() {
        Chunk chunk = world.getChunkFromWorld(loc);
        assertNotNull(chunk);
        assertEquals(chunk, world.getChunkFromWorld(loc));
    }

    @Test
    public void getCorrectChunkFromWorldCoords() {
        Chunk originChunk = world.getChunk(loc);
        for (int x = 0; x < CHUNK_SIZE; x++) {
            Chunk chunk = world.getChunkFromWorld(x, 0);
            assertEquals(originChunk, chunk);
        }
        assertEquals(world.getChunk(-1, 0), world.getChunkFromWorld(-2, 0));
        assertEquals(world.getChunk(-2, 0), world.getChunkFromWorld(-CHUNK_SIZE - 1, 0));
        assertEquals(world.getChunk(-1, 0), world.getChunkFromWorld(-1, 0));
        assertEquals(world.getChunk(0, -1), world.getChunkFromWorld(0, -CHUNK_SIZE + 1));
        assertEquals(world.getChunk(1, 0), world.getChunkFromWorld(CHUNK_SIZE, 0));
        assertEquals(world.getChunk(0, 0), world.getChunkFromWorld(CHUNK_SIZE - 1, 0));
        assertEquals(world.getChunk(1, 0), world.getChunkFromWorld(CHUNK_SIZE + 1, 0));
        assertEquals(world.getChunk(2, 0), world.getChunkFromWorld(CHUNK_SIZE * 2, 0));
    }

    @Test
    public void setCorrectBlockFromOrigin() {
        world.setBlock(0, 0, Material.STONE);
        assertEquals(Material.STONE, world.getChunk(0, 0).getBlock(0, 0).getMaterial());
    }

    @Test
    public void setCorrectBlockFromWorldCoords() {
        world.setBlock(CHUNK_SIZE + 1, 3 * CHUNK_SIZE + 9, Material.STONE);
        assertEquals(Material.STONE, world.getChunk(1, 3).getBlock(1, 9).getMaterial());
    }

    @Test
    public void setCorrectBlockFromWorldCoordsNeg() {
        world.setBlock(-CHUNK_SIZE + 1, -3 * CHUNK_SIZE + 9, Material.STONE);
        assertEquals(Material.STONE, world.getChunk(-1, -3).getBlock(1, 9).getMaterial());
    }

    @Test
    public void getCorrectBlockFromOrigin() {
        world.getChunk(0, 0).setBlock(0, 0, Material.STONE);
        assertEquals(Material.STONE, world.getBlock(0, 0, false).getMaterial());
    }

    @Test
    public void getCorrectBlockFromWorldCoords() {
        world.getChunk(-2, 5).setBlock(2, 11, Material.STONE);
        assertEquals(Material.STONE, world.getBlock(-2 * CHUNK_SIZE + 2, 5 * CHUNK_SIZE + 11, false).getMaterial());
    }

    @Test(expected = IllegalArgumentException.class)
    public void BlockWithinNegativeRadiusFails() {
        world.getBlocksWithin(0, 0, -1, false);
    }

    @Test
    public void BlockAtOriginWithZeroRadiusReturnsEmptySet() {
        ObjectSet<Block> blocks = world.getBlocksWithin(0, 0, 0, false);
        assertTrue(blocks.isEmpty());
    }

    @Test
    public void BlockAtBlockOriginWithZeroRadiusReturnsOriginBlock() {
        ObjectSet<Block> blocks = world.getBlocksWithin(0.5f, 0.5f, 0, false);
        ObjectSet<Block> expected = new ObjectSet<>();
        expected.add(world.getBlock(0, 0, false));
        assertEquals(expected, blocks);
    }

    @Test
    public void BlockWithinOneReturnsGivenCoords() {
        ObjectSet<Block> blocks = world.getBlocksWithin(0, 0, 1, false);

        ObjectSet<Block> expected = new ObjectSet<>();
        expected.add(world.getBlock(0, 0, false));
        expected.add(world.getBlock(-1, 0, false));
        expected.add(world.getBlock(0, -1, false));
        expected.add(world.getBlock(-1, -1, false));

        assertEquals(expected, blocks);
    }

    @Test
    public void BlockWithin1_5ReturnsGivenCoords() {
        ObjectSet<Block> blocks = world.getBlocksWithin(0.5f, 0.5f, 1.5f, false);

        ObjectSet<Block> expected = new ObjectSet<>();
        expected.add(world.getBlock(0, 0, false));
        for (Direction dir : Direction.values()) {
            expected.add(world.getBlock(dir.dx, dir.dy, false));
        }

        assertEquals(expected, blocks);
    }

    @Test
    public void BlockWithinOneOffsetPoint5() {
        ObjectSet<Block> blocks = world.getBlocksWithin(0.5f, 0.5f, 1, false);
        //convert set to the locations of the blocks
        Set<Location> bal = StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(blocks.iterator(), Spliterator.ORDERED), false).map(
            block -> new Location(block.getWorldX(), block.getWorldY())).collect(Collectors.toSet());

        Set<Block> expected = new HashSet<>();
        expected.add(world.getBlock(0, 0, false));
        for (Direction dir : Direction.CARDINAL) {
            expected.add(world.getBlock(dir.dx, dir.dy, false));
        }
        Set<Location> larr = expected.stream().map(block -> new Location(block.getWorldX(), block.getWorldY())).collect(
            Collectors.toSet());

        assertEquals(larr, bal);
    }

    @Test
    public void skyColorForMiddayIsWhite() {
        assertEquals(1, world.getSkyBrightness(World.MIDDAY_TIME), 0);
    }

    @Test
    public void skyColorForMiddayIsWhiteNextDay() {
        assertEquals(1, world.getSkyBrightness(World.MIDDAY_TIME + 360), 0);
    }

    @Test
    public void skyColorForMiddayIsWhitePrevDay() {
        assertEquals(1, world.getSkyBrightness(World.MIDDAY_TIME - 360), 0);
    }

    @Test
    public void skyColorForMidnightIsBlack() {
        assertEquals(0, world.getSkyBrightness(World.MIDNIGHT_TIME), 0);
    }

    @Test
    public void skyColorForMidnightIsBlackNextDay() {
        assertEquals(0, world.getSkyBrightness(World.MIDNIGHT_TIME + 360), 0);
    }

    @Test
    public void skyColorForMidnightIsBlackPrevDay() {
        assertEquals(0, world.getSkyBrightness(World.MIDNIGHT_TIME - 360), 0);
    }


    //////////
    // dawn //
    //////////

    @Test
    public void skyColorAtStartOfDawnIsBlack() {
        assertEquals(0, world.getSkyBrightness(World.TWILIGHT_DEGREES), 0);
    }

    @Test
    public void skyColorDuringStartOfDawnIsHalfGrayHalfBlack() {
        assertEquals(0, world.getSkyBrightness((World.TWILIGHT_DEGREES / 2)), 0);
    }

    @Test
    public void skyColorDawnIsGray() {
        assertEquals(0, world.getSkyBrightness(World.SUNRISE_TIME), 0);
    }

    @Test
    public void skyColorDuringEndOfDawnIsHalfGrayHalfWhite() {
        assertEquals(0.5f, world.getSkyBrightness(-World.TWILIGHT_DEGREES / 2), 0);
    }

    @Test
    public void skyColorAtEndOfDawnIsWhite() {
        assertEquals(1, world.getSkyBrightness(-World.TWILIGHT_DEGREES), 0);
    }


    //////////
    // dusk //
    //////////


    @Test
    public void skyColorAtStartOfDuskIsWhite() {
        assertEquals(1, world.getSkyBrightness(World.SUNSET_TIME), 0);
    }

    @Test
    public void skyColorDuringStartOfDuskIsHalfGrayHalfWhite() {
        assertEquals(0.5f, world.getSkyBrightness(180 + World.TWILIGHT_DEGREES / 2), 0);
    }

    @Test
    public void skyColorDuskIsGray() {
        assertEquals(0, world.getSkyBrightness(180), 0);
    }

    @Test
    public void skyColorDuringEndOfDuskIsHalfGrayHalfBlack() {
        assertEquals(0, world.getSkyBrightness(180 - World.TWILIGHT_DEGREES / 2), 0);
    }

    @Test
    public void skyColorAtEndOfDuskIBlack() {
        assertEquals(0, world.getSkyBrightness(180 - World.TWILIGHT_DEGREES), 0);
    }
}
