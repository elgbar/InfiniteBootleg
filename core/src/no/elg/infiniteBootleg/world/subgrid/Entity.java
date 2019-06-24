package no.elg.infiniteBootleg.world.subgrid;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.Updatable;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * An entity that can move between the main world grid. The position of each entity is recorded in world coordinates.
 */
public abstract class Entity implements Updatable {

    private final Body body;
    private final World world;

    private boolean flying;

    private UUID uuid;


    public Entity(@NotNull World world, float x, float y) {
        this(world, new Vector2(x, y));
    }

    private Entity(@NotNull World world, @NotNull Vector2 position) {
        uuid = UUID.randomUUID();
        world.getEntities().add(this);
        this.world = world;
        flying = false;

        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(position.x, position.y);
        body = getWorld().getRender().getBox2dWorld().createBody(bodyDef);
        body.setFixedRotation(true);

        PolygonShape box = new PolygonShape();
        box.setAsBox(0.5f, 0.5f);
        body.createFixture(box, 1.0f);
        box.dispose();
    }

    @Override
    public void update() {

    }

    /**
     * @return The texture of this entity
     */
    public abstract TextureRegion getTextureRegion();

    /**
     * @return The width of this entity
     */
    public abstract float getWidth();

    /**
     * @return The height of this entity
     */
    public abstract float getHeight();

    public Vector2 getPosition() {
        return body.getPosition();
    }

    public Body getBody() {
        return body;
    }

    public boolean isFlying() {
        return flying;
    }

    public void setFlying(boolean flying) {
        this.flying = flying;
    }

    public World getWorld() {
        return world;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        Entity entity = (Entity) o;
        return uuid.equals(entity.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
