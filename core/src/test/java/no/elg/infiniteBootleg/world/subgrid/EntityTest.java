package no.elg.infiniteBootleg.world.subgrid;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import no.elg.infiniteBootleg.TestGraphic;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.FlatChunkGenerator;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EntityTest extends TestGraphic {

    private World world;
    private Entity entity;

    @Before
    public void before() {
        if (world != null) { world.dispose(); }
        world = new World(new FlatChunkGenerator(), -1);
        entity = new EntityImpl(world, 0, 0);
    }

    @Test
    public void noCollideAirToAir() {
        assertEquals(new Location(0, 0), entity.collide(Vector2.X));
    }

    @Test
    public void noCollideSameLoc() {
        assertEquals(new Location(0, 0), entity.collide(Vector2.X));
    }

    @Test
    public void collideGround() {
        assertEquals(new Location(0, 0), entity.collide(new Vector2(0, -1)));
    }

    @Test
    public void collideTwoBelow() {
        assertEquals(new Location(0, 0), entity.collide(new Vector2(0, -2)));
    }

    @Test
    public void collideTwoBelowAndToTheSide() {
        assertEquals(new Location(0, 0), entity.collide(new Vector2(2, -2)));
    }

    @Test
    public void collideNonZero() {
        entity.getPosition().set(10, 0);
        assertEquals(new Location(10, 0), entity.collide(new Vector2(10, -10)));
    }

    @Test
    public void collideTwoAbove() {
        world.setBlock(1, 1, Material.STONE);
        assertEquals(new Location(0, 0), entity.collide(new Vector2(2, 2)));
    }

    @Test
    public void collideDown() {
        world.setBlock(1, 2, Material.STONE);
        world.setBlock(2, 2, Material.STONE);
        entity.getPosition().set(2, 3);
        assertEquals(new Location(2, 3), entity.collide(new Vector2(0, 0)));
    }

    @Test
    public void weirdCollide() {
        entity.getPosition().set(20, 34);
        Vector2 vel = new Vector2(0, -0.0147704035f);
        assertNull(entity.collide(entity.getPosition().cpy().add(vel)));
    }


    private static class EntityImpl extends Entity {

        private EntityImpl(World world, int x, int y) {
            super(world, x, y);
        }

        @Override
        public TextureRegion getTextureRegion() {
            return null;
        }
    }
}
