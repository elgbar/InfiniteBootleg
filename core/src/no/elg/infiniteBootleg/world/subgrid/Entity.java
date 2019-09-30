package no.elg.infiniteBootleg.world.subgrid;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectSet;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.Ticking;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.box2d.ContactHandler;
import no.elg.infiniteBootleg.world.subgrid.box2d.ContactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * An entity that can move between the main world grid.
 * <p>
 * The position of each entity is recorded in world coordinates and is centered in the middle of the entity.
 */
public abstract class Entity implements Ticking, Disposable, ContactHandler {

    private static final float GROUND_CHECK_OFFSET = 0.25f;

    private final World world;
    private Body body;
    private boolean flying; //ignore world gravity
    private UUID uuid;
    private Vector2 posCache;
    private Vector2 velCache;
    private int groundContacts;
    private Filter filter;

    public Entity(@NotNull World world, float worldX, float worldY) {
        this(world, worldX, worldY, true);
    }

    public Entity(@NotNull World world, float worldX, float worldY, boolean center) {
        uuid = UUID.randomUUID();
        this.world = world;
        flying = false;
        world.addEntity(this);
        posCache = new Vector2(worldX, worldY);
        velCache = new Vector2();
        filter = World.ENTITY_FILTER;

        if (center) {
            posCache.add(getHalfBox2dWidth(), getHalfBox2dHeight());
        }

        if (isInvalidSpawn()) {
            Gdx.app.debug("Entity", //
                          String.format("Did not spawn %s at (% 8.2f,% 8.2f) as the spawn is invalid", //
                                        simpleName(), posCache.x, posCache.y));
            world.removeEntity(this);
            return;
        }

        synchronized (WorldRender.BOX2D_LOCK) {
            BodyDef def = createBodyDef(posCache.x, posCache.y);
            body = world.getWorldBody().createBody(def);
            createFixture(body);
        }
    }

    @NotNull
    protected BodyDef createBodyDef(float worldX, float worldY) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(worldX, worldY);
        bodyDef.fixedRotation = true;
        return bodyDef;
    }

    protected void createFixture(@NotNull Body body) {
        PolygonShape box = new PolygonShape();
        box.setAsBox(getHalfBox2dWidth(), getHalfBox2dHeight());
        Fixture fix = body.createFixture(box, 1.0f);
        fix.setFilterData(World.ENTITY_FILTER);
        box.dispose();
    }

    /**
     * @return If the given location is invalid
     */
    protected boolean isInvalidSpawn() {
        //noinspection LibGDXUnsafeIterator
        for (Block block : touchingBlocks()) {
            if (block.getMaterial() != Material.AIR) {
                return true;
            }
        }
        return !touchingEntities().isEmpty();
    }

    public void teleport(float x, float y, boolean validate) {
        synchronized (WorldRender.BOX2D_LOCK) {
            body.setTransform(x, y, 0);
            body.setAngularVelocity(0);
            body.setLinearVelocity(0, 0);
            body.setAwake(true);

            if (validate && isInvalidSpawn()) {
                Main.inst().getConsoleLogger().error("Entity", String
                    .format("Failed to teleport entity %s to (% 4.2f,% 4.2f) from (% 4.2f,% 4.2f)", toString(), x, y,
                            posCache.x, posCache.y));
                body.setTransform(posCache, 0);
            }
        }
    }

    /**
     * @return A list of all the blocks this entity is touching
     */
    public Array<Block> touchingBlocks() {
        Array<Block> blocks = new Array<>(Block.class);
        int x = MathUtils.floor(posCache.x - getHalfBox2dWidth());
        float maxX = posCache.x + getHalfBox2dWidth();
        for (; x < maxX; x++) {
            int y = MathUtils.floor(posCache.y - getHalfBox2dHeight());
            float maxY = posCache.y + getHalfBox2dHeight();
            for (; y < maxY; y++) {
                blocks.add(world.getBlock(x, y));
            }
        }
        return blocks;
    }

    /**
     * @return A set of all other entities (excluding this) this entity is touching
     */
    public ObjectSet<Entity> touchingEntities() {
        ObjectSet<Entity> entities = new ObjectSet<>();

        for (Entity entity : world.getEntities()) {
            if (entity == this || (getFilter().maskBits & entity.getFilter().categoryBits) == 0 ||
                (entity.getFilter().maskBits & getFilter().categoryBits) == 0) {
                continue;
            }
            Vector2 pos = entity.getPosition();
            boolean bx = Util.isBetween(pos.x - entity.getHalfBox2dWidth(), posCache.x,
                                        pos.x + entity.getHalfBox2dWidth());
            boolean by = Util.isBetween(pos.y - entity.getHalfBox2dHeight(), posCache.y,
                                        pos.y + entity.getHalfBox2dHeight());
            if (bx && by) {
                entities.add(entity);
            }
        }
        return entities;
    }


    @Override
    public void contact(@NotNull ContactType type, @NotNull Contact contact) {
        if (contact.getFixtureA().getFilterData().categoryBits == World.GROUND_CATEGORY) {
            if (type == ContactType.BEGIN_CONTACT) {
                //newest pos is needed to accurately check if this is on ground
                updatePos();
                //y pos - getHalfBox2dHeight is middle
                int y = MathUtils.floor(posCache.y - getHalfBox2dHeight() - GROUND_CHECK_OFFSET);
                if (world.isAir(getBlockX(), y)) { return; }
                groundContacts++;
            }
            else if (type == ContactType.END_CONTACT) {
                groundContacts--;
                if (groundContacts < 0) { groundContacts = 0; }
            }
        }
    }

    /**
     * Update the cached position and velocity
     */
    private void updatePos() {
        if (body == null) { return; }
        synchronized (WorldRender.BOX2D_LOCK) {
            posCache = body.getPosition();
            velCache = body.getLinearVelocity();
        }
    }

    @Override
    public void tick() {
        if (Main.renderGraphic) {
            updatePos();
        }
    }

    /**
     * @return The texture of this entity
     */
    @Nullable
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

    @NotNull
    public Vector2 getPosition() {
        return posCache;
    }

    @NotNull
    public Vector2 getVelocity() {
        return velCache;
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
                body.setGravityScale(0);
            }
            else {
                body.setGravityScale(1);
                body.setAwake(true);
            }
        }
    }

    public World getWorld() {
        return world;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isOnGround() {
        return groundContacts > 0;
    }

    public String simpleName() {
        return getClass().getSimpleName();
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
        if (body != null) {
            synchronized (WorldRender.BOX2D_LOCK) {
                for (Fixture fixture : body.getFixtureList()) {
                    fixture.setFilterData(filter);
                }
            }
        }
    }

    @Override
    public void dispose() {
        world.getWorldBody().destroyBody(body);
        body = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof Entity)) { return false; }
        Entity entity = (Entity) o;
        return uuid.equals(entity.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return "Entity{uuid=" + uuid + '}';
    }
}
