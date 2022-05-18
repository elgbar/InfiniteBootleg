package no.elg.infiniteBootleg.world.subgrid;

import static java.lang.Math.abs;
import static java.lang.Math.signum;
import static no.elg.infiniteBootleg.input.KeyboardControls.MAX_X_VEL;
import static no.elg.infiniteBootleg.input.KeyboardControls.MAX_Y_VEL;
import static no.elg.infiniteBootleg.world.World.NON_INTERACTIVE_FILTER;
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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import com.google.common.base.Preconditions;
import java.util.UUID;
import kotlin.jvm.functions.Function1;
import no.elg.infiniteBootleg.CheckableDisposable;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Ticking;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.server.PacketExtraKt;
import no.elg.infiniteBootleg.server.ServerClient;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.util.ExtraKt;
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
import no.elg.infiniteBootleg.world.subgrid.enitites.FallingBlockEntity;
import no.elg.infiniteBootleg.world.subgrid.enitites.GenericEntity;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An entity that can move between the main world grid.
 *
 * <p>The position of each entity is recorded in world coordinates and is centered in the middle of
 * the entity.
 */
public abstract class Entity
    implements Ticking,
        CheckableDisposable,
        ContactHandler,
        HUDDebuggable,
        Savable<ProtoWorld.EntityOrBuilder> {

  public static final float GROUND_CHECK_OFFSET = 0.1f;
  public static final float TELEPORT_DIFFERENCE_THRESHOLD = .5f;
  public static final float TELEPORT_DIFFERENCE_Y_OFFSET = 0.01f;
  public static final float DEFAULT_GRAVITY_SCALE = 2f;
  public static final long FREEZE_DESPAWN_TIMEOUT_MS = 1000L;
  @NotNull private final World world;
  private final UUID uuid;

  // Note: must only be accessed with getters
  @Nullable private volatile Body body;

  private final Array<Function1<@NotNull Body, @Nullable Void>> bodyCreations = new Array<>();
  private boolean flying; // ignore world gravity
  private final Vector2 posCache;
  private final Vector2 velCache;
  private int groundContacts;
  @NotNull private Filter filter = World.ENTITY_FILTER;
  private float lookDeg;

  protected volatile boolean disposed;

  public Entity(@NotNull World world, @NotNull ProtoWorld.Entity protoEntity) {
    this(
        world,
        protoEntity.getPosition().getX(),
        protoEntity.getPosition().getY(),
        false,
        UUID.fromString(protoEntity.getUuid()),
        false);
    if (isDisposed()) {
      return;
    }
    Preconditions.checkArgument(protoEntity.getType() == getEntityType());
    if (protoEntity.getFlying()) {
      setFlying(true);
    }
    final ProtoWorld.Vector2f velocity = protoEntity.getVelocity();
    if (velocity.getX() != 0f || velocity.getY() != 0f) {
      postWorldBodyRunnable(
          body -> {
            body.setLinearVelocity(velocity.getX(), velocity.getY());
            return null;
          });
    }
  }

  protected Entity(@NotNull World world, float worldX, float worldY, @NotNull UUID uuid) {
    this(world, worldX, worldY, true, uuid);
  }

  protected Entity(
      @NotNull World world, float worldX, float worldY, boolean center, @NotNull UUID uuid) {
    this(world, worldX, worldY, center, uuid, true);
  }

  protected Entity(
      @NotNull World world,
      float worldX,
      float worldY,
      boolean center,
      @NotNull UUID uuid,
      boolean validateLocation) {
    this.uuid = uuid;
    this.world = world;
    posCache = new Vector2(worldX, worldY);
    velCache = new Vector2();

    if (world.containsEntity(uuid)) {
      Main.logger().warn("World already contains entity with uuid " + uuid);
      dispose();
      return;
    }

    if (center) {
      posCache.add(getHalfBox2dWidth(), getHalfBox2dHeight());
    }

    if (validateLocation && isInvalidLocation(posCache.x, posCache.y)) {
      switch (invalidSpawnLocationAction()) {
        case DELETE -> {
          Main.logger()
              .debug(
                  "Entity",
                  String.format(
                      "Did not spawn %s at (%.2f,%.2f) as the spawn is invalid",
                      simpleName(), posCache.x, posCache.y));
          dispose();
          return;
        }
        case PUSH_UP -> {
          // make sure we're not stuck in an infinite loop if the given height is zero
          float checkStep = Math.min(getHalfBox2dHeight(), 0.1f);
          while (isInvalidLocation(posCache.x, posCache.y)) {
            posCache.y += checkStep;
          }
        }
      }
    }

    if (Main.inst().isNotTest()) {
      BodyDef def = createBodyDef(posCache.x, posCache.y);
      world
          .getWorldBody()
          .createBody(
              def,
              newBody -> {
                synchronized (bodyCreations) {
                  setBody(newBody);
                  createFixture(newBody);
                  newBody.setGravityScale(DEFAULT_GRAVITY_SCALE);
                  newBody.setUserData(this);

                  for (Function1<@NotNull Body, @Nullable Void> runnable : bodyCreations) {
                    runnable.invoke(newBody);
                  }
                  bodyCreations.clear();
                }
                return null;
              });
    }

    // Sanity check
    Main.inst()
        .getScheduler()
        .scheduleSync(
            10L,
            () -> {
              if (!isDisposed() && !world.containsEntity(uuid)) {
                Main.logger()
                    .warn(
                        "Failed to find entity "
                            + simpleName()
                            + " '"
                            + hudDebug()
                            + "' uuid "
                            + uuid
                            + " in the world '"
                            + world
                            + "'! Did you forget to add it?");
              }
            });
  }

  protected void postWorldBodyRunnable(Function1<@NotNull Body, @Nullable Void> runnable) {
    boolean runNow;
    Body currentBody = getUnsafeBody();
    synchronized (bodyCreations) {
      runNow = currentBody != null;
      if (!runNow) {
        bodyCreations.add(runnable);
      }
    }
    if (runNow) {
      world
          .getWorldBody()
          .postBox2dRunnable(
              () -> {
                if (isDisposed()) {
                  return;
                }
                runnable.invoke(currentBody);
              });
    }
  }

  @NotNull
  protected BodyDef createBodyDef(float worldX, float worldY) {
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = BodyDef.BodyType.DynamicBody;
    bodyDef.position.set(worldX, worldY);
    bodyDef.linearDamping = 1f;
    bodyDef.fixedRotation = true;
    return bodyDef;
  }

  /** Always call while synchronized with {@link WorldRender#BOX2D_LOCK} */
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
   * @param worldX The x world coordinate to check
   * @param worldY The y world coordinate to check
   * @return {@code true} if the entity is allowed to be at the given location
   */
  public boolean isInvalidLocation(float worldX, float worldY) {
    return !wouldOnlyTouchAir(worldX, worldY) || !touchingEntities(worldX, worldY).isEmpty();
  }

  public void teleport(float worldX, float worldY, boolean validate) {
    translate(worldX, worldY, 0, 0, 0f, validate);
  }

  public void translate(
      float worldX, float worldY, float velX, float velY, float lookAngleDeg, boolean validate) {
    if (isDisposed()) {
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
        Main.logger()
            .error(
                "Entity",
                String.format(
                    "Failed to teleport entity %s to (% 4.2f,% 4.2f) from (% 4.2f,% 4.2f)",
                    this, worldX, worldY, posCache.x, posCache.y));
        return;
      }
    }
    float newWorldY = worldY;

    final WorldBody worldBody = world.getWorldBody();
    float physicsWorldX = worldX + worldBody.getWorldOffsetX();
    float physicsWorldY = worldY + worldBody.getWorldOffsetY();
    setLookDeg(lookAngleDeg);

    world.postBox2dRunnable(
        () -> {
          boolean sendMovePacket = false;
          synchronized (this) {
            var currentBody = getUnsafeBody();
            if (isDisposed() || currentBody == null) {
              return;
            }
            updatePos();
            // If we're too far away teleport the entity to its correct location
            // and add a bit to the y coordinate, so we don't fall through the floor
            if ((Main.isServerClient()
                    && !ClientMain.inst().getServerClient().getUuid().equals(uuid))
                || Math.abs(physicsWorldX - posCache.x) > TELEPORT_DIFFERENCE_THRESHOLD
                || Math.abs(physicsWorldY - posCache.y) > TELEPORT_DIFFERENCE_THRESHOLD) {
              posCache.x = worldX;
              posCache.y = newWorldY;
              currentBody.setTransform(
                  physicsWorldX, physicsWorldY + TELEPORT_DIFFERENCE_Y_OFFSET, 0);
              sendMovePacket = true;
            }
            currentBody.setLinearVelocity(velX, velY);
            velCache.x = velX;
            velCache.y = velY;

            currentBody.setAwake(true);
          }
          if (Main.isServer() && sendMovePacket) {
            //      Main.logger()
            //          .debug(
            //              "server",
            //              "Force updating entity "
            //                  + hudDebug()
            //                  + " to position ("
            //                  + posCache.x
            //                  + ", "
            //                  + posCache.y
            //                  + ")");
            Main.inst()
                .getScheduler()
                .executeAsync(
                    () ->
                        PacketExtraKt.broadcastToInView(
                            PacketExtraKt.clientBoundMoveEntity(this),
                            getBlockX(),
                            getBlockY(),
                            null));
          }
        });
  }

  /**
   * @return An unordered collection of all the blocks this entity is currently touching
   */
  @NotNull
  public ObjectSet<@NotNull Block> touchingBlocks() {
    return touchingBlocks(posCache.x, posCache.y);
  }

  /**
   * @param worldX World x coordinate to pretend the player is at
   * @param worldY World y coordinate to pretend the player is at
   * @return An unordered collection of all the blocks this entity would be touching if it was
   *     located here
   */
  @NotNull
  public ObjectSet<@NotNull Block> touchingBlocks(float worldX, float worldY) {
    var locations = touchingLocations(worldX, worldY);
    ObjectSet<Block> blocks = new ObjectSet<>();
    for (Location location : locations) {
      Block block = world.getRawBlock(location.x, location.y);
      if (block != null) {
        blocks.add(block);
      }
    }
    return blocks;
  }

  /**
   * @return An unordered collection of all the locations this entity is currently touching
   */
  @NotNull
  public ObjectSet<@NotNull Location> touchingLocations() {
    return touchingLocations(posCache.x, posCache.y);
  }

  /**
   * @param worldX World x coordinate to pretend the player is at
   * @param worldY World y coordinate to pretend the player is at
   * @return An unordered collection of all the locations this entity would be touching if it was
   *     located here
   */
  @NotNull
  public ObjectSet<@NotNull Location> touchingLocations(float worldX, float worldY) {
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
   * @param worldX World x coordinate to pretend the player is at
   * @param worldY World y coordinate to pretend the player is at
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
   * @param worldX World x coordinate to pretend the player is at
   * @param worldY World y coordinate to pretend the player is at
   * @return {@code True} if the player would only touch air if stood at the given location
   */
  public boolean wouldOnlyTouchAir(float worldX, float worldY) {
    int x = MathUtils.floor(worldX - getHalfBox2dWidth());
    float maxX = worldX + getHalfBox2dWidth();
    for (; x < maxX; x++) {
      int y = MathUtils.floor(worldY - getHalfBox2dHeight());
      float maxY = worldY + getHalfBox2dHeight();
      for (; y < maxY; y++) {
        var block = world.getRawBlock(x, y);
        if (block == null
            || block.getMaterial() == Material.AIR
            || (block instanceof EntityBlock entityBlock && entityBlock.getEntity() == this)) {
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
  @NotNull
  public ObjectSet<@NotNull Entity> touchingEntities() {
    return touchingEntities(posCache.x, posCache.y);
  }

  /**
   * @param worldX World x coordinate to pretend the player is at
   * @param worldY World y coordinate to pretend the player is at
   * @return A set of all entites this entity would collide with if it was at the given location
   */
  @NotNull
  public ObjectSet<@NotNull Entity> touchingEntities(float worldX, float worldY) {
    ObjectSet<Entity> entities = new ObjectSet<>();

    var rect = new Rectangle(worldX, worldY, getHalfBox2dWidth() * 2, getHalfBox2dHeight() * 2);

    var otherRect = new Rectangle();
    for (Entity entity : world.getEntities()) {
      // ignore entities we do not collide with and self
      if (entity == this
          || (getFilter().maskBits & entity.getFilter().categoryBits) == 0
          || (entity.getFilter().maskBits & getFilter().categoryBits) == 0) {
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

  @NotNull
  public synchronized Filter getFilter() {
    return filter;
  }

  /**
   * @return Position of this entity last tick, note that the same vector is returned each time. You
   *     should not edit this vector
   */
  @NotNull
  public Vector2 getPosition() {
    return posCache;
  }

  @NotNull
  public Vector2 getPhysicsPosition() {
    return posCache
        .cpy()
        .add(world.getWorldBody().getWorldOffsetX(), world.getWorldBody().getWorldOffsetY());
  }

  /**
   * Set the type of filter for this entities fixtures
   *
   * @param filter The type of filter to set
   */
  public void setFilter(@NotNull Filter filter) {
    this.filter = filter;
    postWorldBodyRunnable(
        body -> {
          for (Fixture fixture : body.getFixtureList()) {
            fixture.setFilterData(filter);
          }
          return null;
        });
  }

  /**
   * Must be called while synchronized under WorldRender.BOX2D_LOCK
   *
   * @param type The type of contact
   * @param contact Contact made
   */
  @Override
  public void contact(@NotNull ContactType type, @NotNull Contact contact) {
    if (isDisposed()) {
      return;
    }
    if (contact.getFixtureA().getFilterData().categoryBits == World.GROUND_CATEGORY) {
      if (type == ContactType.BEGIN_CONTACT) {
        // newest pos is needed to accurately check if this is on ground
        updatePos();

        int y = MathUtils.floor(posCache.y - getHalfBox2dHeight() - GROUND_CHECK_OFFSET);

        int leftX = MathUtils.ceil(posCache.x - (2 * getHalfBox2dWidth()));
        int middleX = MathUtils.floor(posCache.x - getHalfBox2dWidth());
        int rightX = MathUtils.ceil(posCache.x - GROUND_CHECK_OFFSET);

        int detected = 0;

        if (!world.isAirBlock(leftX, y)) {
          detected++;
        }
        if (!world.isAirBlock(middleX, y)) {
          detected++;
        }
        if (!world.isAirBlock(rightX, y)) {
          detected++;
        }
        if (detected > 0) {
          groundContacts++;
        }
      } else if (type == ContactType.END_CONTACT) {
        groundContacts--;
        if (groundContacts < 0) {
          groundContacts = 0;
        }
      }
    }
  }

  /** Update the cached position and velocity */
  public final void updatePos() {
    // must sync with box2d lock, as the side effect is what we desire
    synchronized (BOX2D_LOCK) {
      synchronized (this) {
        final Body currentBody = getUnsafeBody();
        if (isDisposed() || currentBody == null) {
          return;
        }
        final WorldBody worldBody = world.getWorldBody();

        posCache
            .set(currentBody.getPosition())
            .sub(worldBody.getWorldOffsetX(), worldBody.getWorldOffsetY());
        velCache.set(currentBody.getLinearVelocity());
      }
    }
  }

  @Override
  public void tick() {
    if (isDisposed()) {
      return;
    }
    float nx;
    boolean tooFastX = abs(velCache.x) > MAX_X_VEL;
    if (tooFastX) {
      nx = signum(velCache.x) * MAX_X_VEL;
    } else {
      nx = velCache.x;
    }

    float ny;
    boolean tooFastY = abs(velCache.y) > MAX_Y_VEL;
    if (tooFastY) {
      ny = signum(velCache.y) * MAX_Y_VEL;
    } else {
      ny = velCache.y;
    }

    if (tooFastX || tooFastY) {

      postWorldBodyRunnable(
          body -> {
            body.setLinearVelocity(nx, ny);
            return null;
          });
    }
    if (Main.isServer()) {
      Main.inst()
          .getScheduler()
          .executeSync(
              () ->
                  PacketExtraKt.broadcastToInView(
                      PacketExtraKt.clientBoundMoveEntity(this),
                      getBlockX(),
                      getBlockY(),
                      // don't send packet to the owning player
                      (channel, credentials) -> credentials.getEntityUUID() != getUuid()));
    }
  }

  /**
   * Freeze the entity, it will not interact with the world anymore
   *
   * @param despawnAfterTimeout Despawn if nothing have happened in {@link
   *     #FREEZE_DESPAWN_TIMEOUT_MS} time
   */
  public void freeze(boolean despawnAfterTimeout) {
    postWorldBodyRunnable(
        body -> {
          setFilter(NON_INTERACTIVE_FILTER);
          body.setLinearVelocity(0, 0);
          body.setGravityScale(0f);
          return null;
        });

    // Remove
    if (despawnAfterTimeout) {

      Main.inst()
          .getScheduler()
          .scheduleSync(
              FREEZE_DESPAWN_TIMEOUT_MS,
              () -> {
                if (isDisposed()) {
                  return;
                }
                if (Main.isServerClient()) {
                  // Ask the server about the entity
                  final ServerClient client = ClientMain.inst().getServerClient();
                  if (client != null) {
                    client.ctx.writeAndFlush(PacketExtraKt.serverBoundEntityRequest(client, uuid));
                    return;
                  }
                }
                world.removeEntity(this);
              });
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
  public Body getBody() {
    var currentBody = getUnsafeBody();
    if (currentBody == null) {
      throw new IllegalStateException("Cannot access the body of an invalid entity!");
    }
    return currentBody;
  }

  @Nullable
  protected Body getUnsafeBody() {
    synchronized (bodyCreations) {
      return body;
    }
  }

  private synchronized void setBody(@Nullable Body newBody) {
    if (isDisposed() && newBody != null) {
      world.getWorldBody().destroyBody(newBody);
    } else {
      Body oldBody;
      synchronized (bodyCreations) {
        oldBody = getUnsafeBody();
        this.body = newBody;
      }
      if (oldBody != null) {
        world.getWorldBody().destroyBody(oldBody);
      }
    }
  }

  public boolean isFlying() {
    return flying;
  }

  public void setFlying(boolean flying) {
    this.flying = flying;
    postWorldBodyRunnable(
        body -> {
          if (flying) {
            body.setLinearVelocity(0, 0);
            body.setGravityScale(0);
          } else {
            body.setGravityScale(DEFAULT_GRAVITY_SCALE);
            body.setAwake(true);
          }
          return null;
        });
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

  /** Do not call directly. Use {@link World#removeEntity(Entity)} */
  @Override
  public synchronized void dispose() {
    if (isDisposed()) {
      Main.logger().error("Entity", "Tried to dispose an already disposed entity " + this);
      return;
    }
    setBody(null); // must be called before disposed is true
    disposed = true;
    if (this instanceof Removable removable) {
      removable.onRemove();
    }
  }

  @Override
  public boolean isDisposed() {
    return disposed;
  }

  @Override
  @NotNull
  public ProtoWorld.Entity.Builder save() {
    final ProtoWorld.Entity.Builder builder = ProtoWorld.Entity.newBuilder();

    synchronized (BOX2D_LOCK) {
      synchronized (this) {
        updatePos();
        builder.setPosition(ProtoWorld.Vector2f.newBuilder().setX(posCache.x).setY(posCache.y));
        builder.setVelocity(ProtoWorld.Vector2f.newBuilder().setX(velCache.x).setY(velCache.y));
      }
    }
    builder.setUuid(uuid.toString());
    builder.setType(getEntityType());
    builder.setFlying(flying);

    return builder;
  }

  @Nullable
  public static Entity load(
      @NotNull World world, @NotNull Chunk chunk, @NotNull ProtoWorld.Entity protoEntity) {
    var uuid = ExtraKt.fromUUIDOrNull(protoEntity.getUuid());
    if (uuid == null) {
      Main.logger().warn(" " + protoEntity.getUuid());
      return null;
    } else if (world.containsEntity(uuid)) {
      Main.logger().warn("World already contains entity with uuid " + uuid);
      return null;
    }
    Entity entity;
    switch (protoEntity.getType()) {
      case GENERIC_ENTITY -> entity = new GenericEntity(world, protoEntity);
      case FALLING_BLOCK -> entity = new FallingBlockEntity(world, chunk, protoEntity);
      case PLAYER -> {
        var player = new Player(world, protoEntity);
        player.disableGravity();
        entity = player;
      }
      case BLOCK -> {
        Preconditions.checkArgument(protoEntity.hasMaterial());
        final ProtoWorld.Entity.Material entityBlock = protoEntity.getMaterial();
        final Material material = Material.fromOrdinal(entityBlock.getMaterialOrdinal());
        return material.createEntity(world, chunk, protoEntity);
      }

      case UNRECOGNIZED -> {
        Main.logger().error("LOAD", "Failed to load entity due to UNRECOGNIZED type");
        return null;
      }
      default -> {
        Main.logger()
            .error(
                "LOAD", "Failed to load entity due to unknown type: " + protoEntity.getTypeValue());
        return null;
      }
    }

    if (entity.isDisposed()) {
      return null;
    }
    world.addEntity(entity, false);

    return entity;
  }

  @NotNull
  protected abstract ProtoWorld.Entity.EntityType getEntityType();

  public float getLookDeg() {
    return lookDeg;
  }

  public void setLookDeg(float lookDeg) {
    this.lookDeg = lookDeg;
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Entity)) {
      return false;
    }
    Entity entity = (Entity) o;
    return uuid.equals(entity.uuid);
  }

  @Override
  public String toString() {
    return "Entity{uuid=" + uuid + '}';
  }
}
