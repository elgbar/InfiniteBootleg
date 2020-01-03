package no.elg.infiniteBootleg.world.subgrid;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectSet;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Ticking;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.contact.ContactHandler;
import no.elg.infiniteBootleg.world.subgrid.contact.ContactType;
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

        //teleport entity upwards till we find a valid location
        boolean print = true;
        //make sure we're not stuck in a infinite loop if the given height is zero
        float checkStep = getHalfBox2dHeight() != 0 ? getHalfBox2dHeight() : 0.1f;
        while (isInvalidLocation(posCache.x, posCache.y)) {
            if (print) {
                Main.logger().debug("Entity", //
                                    String.format("Did not spawn %s at (%.2f,%.2f) as the spawn is invalid", //
                                                  simpleName(), posCache.x, posCache.y));
                print = false;
            }
            posCache.y += checkStep;
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
     * @param worldX
     *     The x world coordinate to check
     * @param worldY
     *     The y world coordinate to check
     *
     * @return {@code true} if the entity is allowed to be at the given location
     */
    public boolean isInvalidLocation(float worldX, float worldY) {
        return !wouldOnlyTouchAir(worldX, worldY) || !touchingEntities(worldX, worldY).isEmpty();
    }

    public void teleport(float worldX, float worldY, boolean validate) {
        if (validate && isInvalidLocation(worldX, worldY)) {
            Main.logger().error("Entity", String
                .format("Failed to teleport entity %s to (% 4.2f,% 4.2f) from (% 4.2f,% 4.2f)", toString(), worldX,
                        worldY, posCache.x, posCache.y));
            return;
        }

        synchronized (WorldRender.BOX2D_LOCK) {
            body.setTransform(worldX, worldY, 0);
            body.setAngularVelocity(0);
            body.setLinearVelocity(0, 0);
            body.setAwake(true);
            posCache.x = worldX;
            posCache.y = worldY;
        }
    }

    /**
     * @return An unordered collection of all the blocks this entity is currently touching
     */
    public ObjectSet<Block> touchingBlocks() {
        return touchingBlocks(posCache.x, posCache.y);
    }

    /**
     * @param worldX
     *     World x coordinate to pretend the player is at
     * @param worldY
     *     World y coordinate to pretend the player is at
     *
     * @return An unordered collection of all the blocks this entity would be touching if it was located here
     */
    @NotNull
    public ObjectSet<Block> touchingBlocks(float worldX, float worldY) {
        ObjectSet<Block> blocks = new ObjectSet<>();
        int x = MathUtils.floor(worldX - getHalfBox2dWidth());
        float maxX = worldX + getHalfBox2dWidth();
        for (; x < maxX; x++) {
            int y = MathUtils.floor(worldY - getHalfBox2dHeight());
            float maxY = worldY + getHalfBox2dHeight();
            for (; y < maxY; y++) {
                blocks.add(world.getBlock(x, y, false));
            }
        }
        return blocks;
    }

    /**
     * @param worldX
     *     World x coordinate to pretend the player is at
     * @param worldY
     *     World y coordinate to pretend the player is at
     *
     * @return {@code True} if the player would only touch air if stood at the given location
     */
    public boolean wouldOnlyTouchAir(float worldX, float worldY) {
        int x = MathUtils.floor(worldX - getHalfBox2dWidth());
        float maxX = worldX + getHalfBox2dWidth();
        for (; x < maxX; x++) {
            int y = MathUtils.floor(worldY - getHalfBox2dHeight());
            float maxY = worldY + getHalfBox2dHeight();
            for (; y < maxY; y++) {
                if (!world.isAir(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return A set of all other entities (excluding this) this entity is touching
     */
    public ObjectSet<Entity> touchingEntities() {
        return touchingEntities(posCache.x, posCache.y);
    }

    /**
     * @param worldX
     *     World x coordinate to pretend the player is at
     * @param worldY
     *     World y coordinate to pretend the player is at
     *
     * @return A set of all entites this entity would collide with if it was at the given location
     */
    public ObjectSet<Entity> touchingEntities(float worldX, float worldY) {
        ObjectSet<Entity> entities = new ObjectSet<>();

        for (Entity entity : world.getEntities()) {
            //ignore entities we do not collide with and self
            if (entity == this || (getFilter().maskBits & entity.getFilter().categoryBits) == 0 ||
                (entity.getFilter().maskBits & getFilter().categoryBits) == 0) {
                continue;
            }

            Vector2 pos = entity.getPosition();
            //exclude equality of lower bond
            boolean bx = Util.isBetween((pos.x + MathUtils.FLOAT_ROUNDING_ERROR) - entity.getHalfBox2dWidth(), worldX,
                                        pos.x + entity.getHalfBox2dWidth());
            boolean by = Util.isBetween((pos.y + MathUtils.FLOAT_ROUNDING_ERROR) - entity.getHalfBox2dHeight(), worldY,
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
        updatePos();
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

    /**
     * @return Position of this entity last tick, changing this have no impact of the body of this entity
     */
    @NotNull
    public Vector2 getPosition() {
        return posCache.cpy();
    }

    /**
     * @return Velocity of this entity last tick, changing this have no impact of the body of this entity
     */
    @NotNull
    public Vector2 getVelocity() {
        return velCache.cpy();
    }

    /**
     * @return Block x-coordinate of this entity
     */
    public int getBlockX() {
        return MathUtils.floor(posCache.x);
    }

    /**
     * @return Block y-coordinate of this entity
     */
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

    /**
     * Set the type of filter for this entities fixtures
     *
     * @param filter
     *     The type of filter to set
     */
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
