package no.elg.infiniteBootleg.world.subgrid;

import static java.lang.Math.abs;
import static java.lang.Math.signum;
import static no.elg.infiniteBootleg.input.KeyboardControls.MAX_X_VEL;
import static no.elg.infiniteBootleg.input.KeyboardControls.MAX_Y_VEL;
import static no.elg.infiniteBootleg.world.render.WorldRender.BOX2D_LOCK;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectSet;
import com.google.common.base.Preconditions;
import java.util.UUID;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Ticking;
import no.elg.infiniteBootleg.protobuf.Proto;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.util.HUDDebuggable;
import no.elg.infiniteBootleg.util.Savable;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.blocks.EntityBlock;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.contact.ContactHandler;
import no.elg.infiniteBootleg.world.subgrid.contact.ContactType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An entity that can move between the main world grid.
 * <p>
 * The position of each entity is recorded in world coordinates and is centered in the middle of the entity.
 */
public abstract class Entity implements Ticking, Disposable, ContactHandler, HUDDebuggable, Savable<Proto.EntityOrBuilder> {

    public static final float GROUND_CHECK_OFFSET = 0.1f;

    private final World world;
    private final UUID uuid;

    @Nullable
    private Body body;
    private boolean flying; //ignore world gravity
    private final Vector2 posCache;
    private final Vector2 velCache;
    private int groundContacts;
    private Filter filter;

    private volatile boolean valid = true;


    public Entity(@NotNull World world, @NotNull Proto.Entity protoEntity) {
        this(world, protoEntity.getPosition().getX(), protoEntity.getPosition().getY(), false, UUID.fromString(protoEntity.getUuid()));

        Preconditions.checkArgument(protoEntity.getType() == getEntityType());
        if (protoEntity.getFlying()) {
            setFlying(true);
        }
        final Proto.Vector2f velocity = protoEntity.getVelocity();
        synchronized (BOX2D_LOCK) {
            if (isInvalid()) {
                return;
            }
            getBody().setLinearVelocity(velocity.getX(), velocity.getY());
        }
    }

    public Entity(@NotNull World world, float worldX, float worldY, @NotNull UUID uuid) {
        this(world, worldX, worldY, true, uuid);
    }

    public Entity(@NotNull World world, float worldX, float worldY, boolean center, @NotNull UUID uuid) {
        this.uuid = uuid;
        this.world = world;
        posCache = new Vector2(worldX, worldY);
        velCache = new Vector2();

        if (world.containsEntity(uuid)) {
            valid = false;
            return;
        }

        flying = false;
        filter = World.ENTITY_FILTER;

        if (center) {
            posCache.add(getHalfBox2dWidth(), getHalfBox2dHeight());
        }

        synchronized (BOX2D_LOCK) {
            BodyDef def = createBodyDef(posCache.x, posCache.y);
            body = world.getWorldBody().createBody(def);
            createFixture(body);
            body.setGravityScale(2f);
            body.setUserData(this);
        }

        Main.inst().getScheduler().scheduleAsync(() -> {
            updatePos();
            if (isInvalidLocation(posCache.x, posCache.y)) {
                switch (invalidSpawnLocationAction()) {
                    case DELETE -> {
                        Main.logger().debug("Entity",
                                            String.format("Did not spawn %s at (%.2f,%.2f) as the spawn is invalid", simpleName(), posCache.x, posCache.y));
                        getWorld().removeEntity(this);
                        return;
                    }
                    case PUSH_UP -> {
                        //make sure we're not stuck in an infinite loop if the given height is zero
                        float checkStep = Math.min(getHalfBox2dHeight(), 0.1f);
                        Vector2 tmpPos = posCache.cpy();
                        while (isInvalidLocation(tmpPos.x, tmpPos.y)) {
                            tmpPos.y += checkStep;
                        }

                        synchronized (BOX2D_LOCK) {
                            body.setTransform(tmpPos, 0f);
                            posCache.y = tmpPos.y;
                        }
                    }
                }
            }
        }, 1L);
    }

    /**
     * Always call while synchronized with {@link WorldRender#BOX2D_LOCK}
     */
    @NotNull
    protected BodyDef createBodyDef(float worldX, float worldY) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(worldX, worldY);
        bodyDef.linearDamping = 1f;
        bodyDef.fixedRotation = true;
        bodyDef.bullet = true; //glitching through world == bad!
        return bodyDef;
    }

    /**
     * Always call while synchronized with {@link WorldRender#BOX2D_LOCK}
     */
    protected void createFixture(@NotNull Body body) {
        PolygonShape shape = new PolygonShape();

        shape.setAsBox(getHalfBox2dWidth(), getHalfBox2dHeight());

        FixtureDef def = new FixtureDef();
        def.shape = shape;
        def.density = 1000f;
        def.friction = 10f;
        def.restitution = 0.025f; // a bit bouncy!

        Fixture fix = body.createFixture(def);
        fix.setFilterData(World.ENTITY_FILTER);

        shape.dispose();
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
        if (isInvalid()) {
            return;
        }
        if (validate) {
            int tries = getHeight() / Block.BLOCK_SIZE;
            boolean invalid = true;
            for (int y = tries - 1; y >= -tries; y--) {
                if (!isInvalidLocation(worldX, worldY + y)) {
                    worldY += y;
                    invalid = false;
                    break;
                }
            }
            if (invalid) {
                Main.logger().error("Entity",
                                    String.format("Failed to teleport entity %s to (% 4.2f,% 4.2f) from (% 4.2f,% 4.2f)", this, worldX, worldY, posCache.x,
                                                  posCache.y));
                return;
            }
        }

        final WorldBody worldBody = world.getWorldBody();
        float physicsWorldX = worldX + worldBody.getWorldOffsetX();
        float physicsWorldY = worldY + worldBody.getWorldOffsetY();
        synchronized (BOX2D_LOCK) {
            synchronized (this) {
                if (isInvalid()) {
                    return;
                }
                body.setTransform(physicsWorldX, physicsWorldY, 0);
                body.setAngularVelocity(0);
                body.setLinearVelocity(0, 0);
                body.setAwake(true);
            }
        }
        posCache.x = worldX;
        posCache.y = worldY;
        velCache.x = 0;
        velCache.y = 0;
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
        var locations = touchingLocations(worldX, worldY);
        ObjectSet<Block> blocks = new ObjectSet<>();
        for (Location location : locations) {
            blocks.add(world.getBlock(location.x, location.y, false));
        }
        return blocks;
    }

    /**
     * @return An unordered collection of all the locations this entity is currently touching
     */
    public ObjectSet<Location> touchingLocations() {
        return touchingLocations(posCache.x, posCache.y);
    }

    /**
     * @param worldX
     *     World x coordinate to pretend the player is at
     * @param worldY
     *     World y coordinate to pretend the player is at
     *
     * @return An unordered collection of all the locations this entity would be touching if it was located here
     */
    @NotNull
    public ObjectSet<Location> touchingLocations(float worldX, float worldY) {
        ObjectSet<Location> blocks = new ObjectSet<>();
        int x = MathUtils.floor(worldX - getHalfBox2dWidth());
        float maxX = worldX + getHalfBox2dWidth();
        for (; x < maxX; x++) {
            int y = MathUtils.floor(worldY - getHalfBox2dHeight());
            float maxY = worldY + getHalfBox2dHeight();
            for (; y < maxY; y++) {
                blocks.add(new Location(x, y));
            }
        }
        return blocks;
    }

    public float getHalfBox2dWidth() {
        return getWidth() / (Block.BLOCK_SIZE * 2f);
    }

    public float getHalfBox2dHeight() {
        return getHeight() / (Block.BLOCK_SIZE * 2f);
    }

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

    /**
     * @param worldX
     *     World x coordinate to pretend the player is at
     * @param worldY
     *     World y coordinate to pretend the player is at
     *
     * @return {@code True} if the given location is valid
     */
    public boolean validLocation(float worldX, float worldY) {
        int x = MathUtils.floor(worldX - getHalfBox2dWidth());
        float maxX = worldX + getHalfBox2dWidth();
        for (; x < maxX; x++) {
            int y = MathUtils.floor(worldY - getHalfBox2dHeight());
            float maxY = worldY + getHalfBox2dHeight();
            for (; y < maxY; y++) {
                if (!world.canPassThrough(x, y)) {
                    return false;
                }
            }
        }
        return true;
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
                var block = world.getBlock(x, y, true);
                if (block == null || block.getMaterial() == Material.AIR || (block instanceof EntityBlock entityBlock && entityBlock.getEntity() == this)) {
                    continue;
                }
                return false;
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

        var rect = new Rectangle(worldX, worldY, getHalfBox2dWidth() * 2, getHalfBox2dHeight() * 2);

        var otherRect = new Rectangle();
        for (Entity entity : world.getEntities()) {
            //ignore entities we do not collide with and self
            if (entity == this || (getFilter().maskBits & entity.getFilter().categoryBits) == 0 ||
                (entity.getFilter().maskBits & getFilter().categoryBits) == 0) {
                continue;
            }

            Vector2 pos = entity.getPosition();
            otherRect.set(pos.x, pos.y, entity.getHalfBox2dWidth() * 2, entity.getHalfBox2dHeight() * 2);
            if (rect.overlaps(otherRect)) {
                entities.add(entity);
            }
        }
        return entities;
    }

    /**
     * @return Current chunk this entity is in
     */
    @Nullable
    public Chunk getChunk() {
        var chunkX = CoordUtil.worldToChunk(getBlockX());
        var chunkY = CoordUtil.worldToChunk(getBlockY());
        return world.getChunk(chunkX, chunkY);
    }

    public synchronized Filter getFilter() {
        return filter;
    }

    /**
     * @return Position of this entity last tick, note that the same vector is returned each time. You should not edit
     * this vector
     */
    @NotNull
    public Vector2 getPosition() {
        return posCache;
    }

    @NotNull
    public Vector2 getPhysicsPosition() {
        return posCache.cpy().add(world.getWorldBody().getWorldOffsetX(), world.getWorldBody().getWorldOffsetY());
    }

    /**
     * Set the type of filter for this entities fixtures
     *
     * @param filter
     *     The type of filter to set
     */
    public void setFilter(Filter filter) {
        synchronized (BOX2D_LOCK) {
            synchronized (this) {
                if (isInvalid()) { return; }
                this.filter = filter;
                for (Fixture fixture : body.getFixtureList()) {
                    fixture.setFilterData(filter);
                }
            }
        }
    }

    /**
     * Must be called while synchronized under WorldRender.BOX2D_LOCK
     *
     * @param type
     *     The type of contact
     * @param contact
     *     Contact made
     */
    @Override
    public void contact(@NotNull ContactType type, @NotNull Contact contact) {
        if (isInvalid()) {
            return;
        }
        if (contact.getFixtureA().getFilterData().categoryBits == World.GROUND_CATEGORY) {
            if (type == ContactType.BEGIN_CONTACT) {
                //newest pos is needed to accurately check if this is on ground
                updatePos();

                int y = MathUtils.floor(posCache.y - getHalfBox2dHeight() - GROUND_CHECK_OFFSET);

                int leftX = MathUtils.ceil(posCache.x - (2 * getHalfBox2dWidth()));
                int middleX = MathUtils.floor(posCache.x - getHalfBox2dWidth());
                int rightX = MathUtils.ceil(posCache.x - GROUND_CHECK_OFFSET);

                int detected = 0;

                if (!world.isAirBlock(leftX, y)) { detected++; }
                if (!world.isAirBlock(middleX, y)) { detected++; }
                if (!world.isAirBlock(rightX, y)) { detected++; }
                if (detected > 0) {
                    groundContacts++;
                }
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
    public final void updatePos() {
        synchronized (BOX2D_LOCK) {
            synchronized (this) {
                if (isInvalid()) { return; }
                final WorldBody worldBody = world.getWorldBody();
                posCache.set(this.body.getPosition()).sub(worldBody.getWorldOffsetX(), worldBody.getWorldOffsetY());
                velCache.set(this.body.getLinearVelocity());
            }
        }
    }

    @Override
    public void tick() {
        if (isInvalid()) {
            return;
        }
        updatePos();
        float nx;
        boolean tooFastX = abs(velCache.x) > MAX_X_VEL;
        if (tooFastX) {
            nx = signum(velCache.x) * MAX_X_VEL;
        }
        else { nx = velCache.x; }

        float ny;
        boolean tooFastY = abs(velCache.y) > MAX_Y_VEL;
        if (tooFastY) {
            ny = signum(velCache.y) * MAX_Y_VEL;
        }
        else { ny = velCache.y; }

        if (tooFastX || tooFastY) {
            synchronized (BOX2D_LOCK) {
                synchronized (this) {
                    if (isInvalid()) { return; }
                    body.setLinearVelocity(nx, ny);
                }
            }
        }
    }

    /**
     * @return The texture of this entity
     */
    @Nullable
    public abstract TextureRegion getTextureRegion();

    /**
     * @return How to handle invalid spawn location
     */
    @Contract(pure = true)
    public InvalidSpawnAction invalidSpawnLocationAction() {
        return InvalidSpawnAction.DELETE;
    }

    /**
     * @return Velocity of this entity last tick, note that the same vector is returned each time
     */
    @NotNull
    public Vector2 getVelocity() {
        return velCache;
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

    @NotNull
    public synchronized Body getBody() {
        if (isInvalid()) {
            throw new IllegalStateException("Cannot access the body of an invalid entity!");
        }
        return body;
    }

    public boolean isFlying() {
        return flying;
    }

    public void setFlying(boolean flying) {
        synchronized (BOX2D_LOCK) {
            synchronized (this) {
                if (isInvalid()) {
                    return;
                }
                this.flying = flying;
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

    /**
     * Do not call directly. Use {@link World#removeEntity(Entity)}
     */
    @Override
    public void dispose() {
        synchronized (BOX2D_LOCK) {
            synchronized (this) {
                if (isInvalid()) {
                    Main.logger().error("Entity", "Tried to dispose an already disposed entity " + this);
                    return;
                }
                valid = false;
                world.getWorldBody().destroyBody(body);
                body = null;
                if (this instanceof Removable removable) {
                    removable.onRemove();
                }
            }
        }
    }

    @Override
    public Proto.Entity.Builder save() {
        final Proto.Entity.Builder builder = Proto.Entity.newBuilder();

        synchronized (BOX2D_LOCK) {
            synchronized (this) {
                updatePos();
                builder.setPosition(Proto.Vector2f.newBuilder().setX(posCache.x).setY(posCache.y));
                builder.setVelocity(Proto.Vector2f.newBuilder().setX(velCache.x).setY(velCache.y));
            }
        }
        builder.setUuid(uuid.toString());
        builder.setType(getEntityType());
        builder.setFlying(flying);

        return builder;
    }

    @NotNull
    protected abstract Proto.Entity.EntityType getEntityType();

    public boolean isInvalid() {
        return !valid;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof Entity)) { return false; }
        Entity entity = (Entity) o;
        return uuid.equals(entity.uuid);
    }

    @Override
    public String toString() {
        return "Entity{uuid=" + uuid + '}';
    }
}
