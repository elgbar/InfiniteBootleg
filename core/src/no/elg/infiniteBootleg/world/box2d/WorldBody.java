package no.elg.infiniteBootleg.world.box2d;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.WorldTicker;
import no.elg.infiniteBootleg.world.render.Ticking;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.box2d.ContactManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper for {@link com.badlogic.gdx.physics.box2d.World} for asynchronous reasons
 *
 * @author Elg
 */
public class WorldBody implements Ticking {

    private final com.badlogic.gdx.physics.box2d.World box2dWorld;

    public WorldBody(@NotNull World world) {
        synchronized (WorldRender.BOX2D_LOCK) {
            box2dWorld = new com.badlogic.gdx.physics.box2d.World(new Vector2(0f, -10), true);
            box2dWorld.setContactListener(new ContactManager(world));
        }
    }

    @NotNull
    public Body createBody(@NotNull BodyDef def) {
        synchronized (WorldRender.BOX2D_LOCK) {
            return box2dWorld.createBody(def);
        }
    }

    public void destroyBody(@Nullable Body body) {
        if (body == null) {
            return;
        }
        synchronized (WorldRender.BOX2D_LOCK) {
            box2dWorld.destroyBody(body);
        }
    }


    @Override
    public void tick() {
        synchronized (WorldRender.BOX2D_LOCK) {
            box2dWorld.step(WorldTicker.SECONDS_DELAY_BETWEEN_TICKS, 8, 3);
        }
    }

    public com.badlogic.gdx.physics.box2d.World getBox2dWorld() {
        return box2dWorld;
    }
}
