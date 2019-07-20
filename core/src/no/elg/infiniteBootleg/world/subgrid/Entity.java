package no.elg.infiniteBootleg.world.subgrid;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.Updatable;
import no.elg.infiniteBootleg.world.render.WorldRender;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * An entity that can move between the main world grid. The position of each entity is recorded in world coordinates.
 */
public abstract class Entity implements Updatable, Disposable {

    private final World world;
    private Body body;
    private boolean flying;
    private UUID uuid;
    private Vector2 posCache;

    public Entity(@NotNull World world, float worldX, float worldY) {
        uuid = UUID.randomUUID();
        this.world = world;
        flying = false;
        world.addEntity(this);

        if (Main.renderGraphic) {
            synchronized (WorldRender.BOX2D_LOCK) {
                body = createBody(worldX, worldY);
                createFixture(body);
            }
        }
        update();
    }

    @NotNull
    protected Body createBody(float worldX, float worldY) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(worldX, worldY);
        Body body = getWorld().getRender().getBox2dWorld().createBody(bodyDef);
        body.setFixedRotation(true);
        body.setAwake(true);
        return body;
    }

    protected void createFixture(@NotNull Body body) {
        PolygonShape box = new PolygonShape();
        box.setAsBox(getHalfBox2dWidth(), getHalfBox2dHeight());
        Fixture fix = body.createFixture(box, 1.0f);
        fix.setFilterData(World.ENTITY_FILTER);
        box.dispose();
    }

    @Override
    public void update() {
        if (Main.renderGraphic) {
            posCache = body.getPosition();
        }
    }

    /**
     * @return The texture of this entity
     */
    public abstract TextureRegion getTextureRegion();

    /**
     * One unit is {@link Block#BLOCK_SIZE}
     *
     * @return The width of this entity in world view
     */
    public abstract int getWidth();

    /**
     * One unit is {@link Block#BLOCK_SIZE}
     *
     * @return The height of this entity in world view
     */
    public abstract int getHeight();

    public float getHalfBox2dWidth() {
        return getWidth() / (Block.BLOCK_SIZE * 2f);
    }

    public float getHalfBox2dHeight() {
        return getHeight() / (Block.BLOCK_SIZE * 2f);
    }

    public Vector2 getPosition() {
        return posCache;
    }

    public int getBlockX() {
        return MathUtils.floor(posCache.x);
    }

    public int getBlockY() {
        return MathUtils.floor(posCache.y);
    }

    public Body getBody() {
        return body;
    }

    public boolean isFlying() {
        return flying;
    }

    public void setFlying(boolean flying) {
        this.flying = flying;
        synchronized (WorldRender.BOX2D_LOCK) {
            if (flying) {
                body.setLinearVelocity(0, 0);
                body.setType(BodyDef.BodyType.StaticBody);
            }
            else {
                body.setType(BodyDef.BodyType.DynamicBody);
            }
        }
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
            synchronized (WorldRender.BOX2D_LOCK) {
                getWorld().getRender().getBox2dWorld().destroyBody(body);
            }
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
