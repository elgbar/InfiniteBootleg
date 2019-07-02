package no.elg.infiniteBootleg.world.subgrid;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.Updatable;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * An entity that can move between the main world grid. The position of each entity is recorded in world coordinates.
 */
public abstract class Entity implements Updatable, Disposable {

    private Body body;
    private final World world;

    private boolean flying;

    private UUID uuid;

    public Entity(@NotNull World world, float worldX, float worldY) {
        uuid = UUID.randomUUID();
        this.world = world;
        flying = false;

        world.addEntity(this);

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(worldX + getBox2dWidth() / 2, worldY);
        body = getWorld().getRender().getBox2dWorld().createBody(bodyDef);
        body.setFixedRotation(true);

        PolygonShape box = new PolygonShape();
        box.setAsBox(getBox2dWidth() / 2, getBox2dHeight() / 2);
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
     * One unit is {@link Block#BLOCK_SIZE}
     *
     * @return The width of this entity in blockss
     */
    public abstract float getWidth();

    public float getBox2dWidth() {
        return getWidth() / Block.BLOCK_SIZE;
    }

    /**
     * One unit is {@link Block#BLOCK_SIZE}
     *
     * @return The height of this entity in blocks
     */
    public abstract float getHeight();

    public float getBox2dHeight() {
        return getHeight() / Block.BLOCK_SIZE;
    }

    public Vector2 getPosition() {
        return body.getPosition();
    }

    /**
     * @return The current block position of this entity
     */
    public Location getBlockPosition() {
        Vector2 pos = getPosition();
        return new Location((int) Math.floor(pos.x), (int) Math.floor(pos.y));
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
    public void dispose() {
        if (body != null) {
            getWorld().getRender().getBox2dWorld().destroyBody(body);
            body = null;
        }
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
