package no.elg.infiniteBootleg.world;

import static java.lang.Math.abs;
import static no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason.UNKNOWN_REASON;
import static no.elg.infiniteBootleg.protobuf.ProtoWorld.World.Generator.EMPTY;
import static no.elg.infiniteBootleg.protobuf.ProtoWorld.World.Generator.FLAT;
import static no.elg.infiniteBootleg.protobuf.ProtoWorld.World.Generator.PERLIN;
import static no.elg.infiniteBootleg.protobuf.ProtoWorld.World.Generator.UNRECOGNIZED;
import static no.elg.infiniteBootleg.world.GlobalLockKt.BOX2D_LOCK;
import static no.elg.infiniteBootleg.world.ecs.AshleyKt.disposeEntitiesOnRemoval;
import static no.elg.infiniteBootleg.world.ecs.AshleyKt.ensureUniquenessListener;
import static no.elg.infiniteBootleg.world.ecs.EntityCreationFactoryKt.createSPPlayerEntity;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.LongArray;
import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.google.common.base.Preconditions;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.concurrent.GuardedBy;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.api.Resizable;
import no.elg.infiniteBootleg.events.InitialChunksOfWorldLoadedEvent;
import no.elg.infiniteBootleg.events.WorldLoadedEvent;
import no.elg.infiniteBootleg.events.api.EventManager;
import no.elg.infiniteBootleg.events.chunks.ChunkLoadedEvent;
import no.elg.infiniteBootleg.protobuf.Packets;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.server.ServerHelperKt;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.util.DebugUtilsKt;
import no.elg.infiniteBootleg.util.ExtraKt;
import no.elg.infiniteBootleg.util.Ticker;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.world.blocks.TickingBlock;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import no.elg.infiniteBootleg.world.ecs.AshleyKt;
import no.elg.infiniteBootleg.world.ecs.components.required.Box2DBodyComponent;
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent;
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent;
import no.elg.infiniteBootleg.world.ecs.system.ControlSystem;
import no.elg.infiniteBootleg.world.ecs.system.FollowEntitySystem;
import no.elg.infiniteBootleg.world.ecs.system.MaxVelocitySystem;
import no.elg.infiniteBootleg.world.ecs.system.ReadBox2DStateSystem;
import no.elg.infiniteBootleg.world.ecs.system.WriteBox2DStateSystem;
import no.elg.infiniteBootleg.world.ecs.system.event.InputSystem;
import no.elg.infiniteBootleg.world.ecs.system.event.PhysicsSystem;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import no.elg.infiniteBootleg.world.generator.EmptyChunkGenerator;
import no.elg.infiniteBootleg.world.generator.FlatChunkGenerator;
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator;
import no.elg.infiniteBootleg.world.loader.ChunkLoader;
import no.elg.infiniteBootleg.world.loader.WorldLoader;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.ticker.WorldTicker;
import no.elg.infiniteBootleg.world.time.WorldTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Different kind of views
 *
 * <ul>
 *   <li>Chunk view: One unit in chunk view is {@link Chunk#CHUNK_SIZE} times larger than a unit in
 *       world view
 *   <li>World view: One unit in world view is {@link Block#BLOCK_SIZE} times larger than a unit in
 *       Box2D view
 *   <li>Box2D view: 1 (ie base unit)
 * </ul>
 *
 * @author Elg
 */
public abstract class World implements Disposable, Resizable {

  public static final float HALF_BLOCK_SIZE = 0.5f;
  public static final float BLOCK_SIZE = 1f;
  public static final float LIGHT_SOURCE_LOOK_BLOCKS = 7.5f;

  public static final int TRY_LOCK_CHUNKS_DURATION_MS = 100;

  @NotNull private final String uuid;
  private final long seed;
  @NotNull private final WorldTicker worldTicker;
  @NotNull private final ChunkLoader chunkLoader;
  @NotNull private final WorldBody worldBody;
  @NotNull private final WorldTime worldTime;

  @NotNull private final Engine engine;

  /** must be accessed under {@link #chunksLock} */
  @GuardedBy("chunksLock")
  @NotNull
  protected final LongMap<Chunk> chunks = new LongMap<>();

  /** Must be accessed under {@code synchronized(chunkColumns)} */
  @NotNull protected final IntMap<ChunkColumn> chunkColumns = new IntMap<>();

  @Nullable private volatile FileHandle worldFile;

  @NotNull private final String name;

  /** Spawn in world coordinates */
  private Location spawn;

  public final ReadWriteLock chunksLock = new ReentrantReadWriteLock();

  private boolean transientWorld = !Settings.loadWorldFromDisk || Main.isServerClient();

  public World(@NotNull ProtoWorld.World protoWorld) {
    this(WorldLoader.generatorFromProto(protoWorld), protoWorld.getSeed(), protoWorld.getName());
  }

  public World(@NotNull ChunkGenerator generator, long seed, @NotNull String worldName) {
    this.seed = seed;
    MathUtils.random.setSeed(seed);
    uuid = String.valueOf(ExtraKt.generateUUIDFromName("" + seed));

    name = worldName;

    worldTicker = new WorldTicker(this, false);

    chunkLoader = new ChunkLoader(this, generator);
    worldTime = new WorldTime(this);
    spawn = new Location(0, 0);
    engine = initializeEngine();
    worldBody = new WorldBody(this);
  }

  private Engine initializeEngine() {
    var engine = new PooledEngine();
    engine.addSystem(MaxVelocitySystem.INSTANCE);
    engine.addSystem(ReadBox2DStateSystem.INSTANCE);
    engine.addSystem(WriteBox2DStateSystem.INSTANCE);
    engine.addSystem(InputSystem.INSTANCE);
    engine.addSystem(ControlSystem.INSTANCE);
    engine.addSystem(PhysicsSystem.INSTANCE);
    if (Main.isClient()) {
      engine.addSystem(FollowEntitySystem.INSTANCE);
    }

    ensureUniquenessListener(engine);
    disposeEntitiesOnRemoval(engine);

    return engine;
  }

  public void initialize() {
    if (Settings.loadWorldFromDisk) {
      FileHandle worldFolder = getWorldFolder();
      if (!transientWorld
          && worldFolder != null
          && worldFolder.isDirectory()
          && !WorldLoader.canWriteToWorld(uuid)) {
        if (!Settings.ignoreWorldLock) {
          transientWorld = true;
          Main.logger()
              .warn("World", "World found is already in use. Initializing world as a transient.");
        } else {
          Main.logger()
              .warn(
                  "World",
                  "World found is already in use. However, ignore world lock is enabled therefore"
                      + " the world will be loaded normally. Here be corrupt worlds!");
        }
      }

      if (transientWorld || worldFolder == null) {
        Main.logger().log("No world save found");
      } else {
        Main.logger().log("Loading world from '" + worldFolder.file().getAbsolutePath() + '\'');

        if (WorldLoader.writeLockFile(uuid)) {
          var worldInfoFile = worldFolder.child(WorldLoader.WORLD_INFO_PATH);
          if (worldInfoFile.exists() && !worldInfoFile.isDirectory()) {
            try {
              ProtoWorld.World protoWorld = ProtoWorld.World.parseFrom(worldInfoFile.readBytes());
              loadFromProtoWorld(protoWorld);
            } catch (InvalidProtocolBufferException e) {
              e.printStackTrace();
            }
          }
        } else {
          Main.logger()
              .error("Failed to write world lock file! Setting world to transient to be safe");
          transientWorld = true;
        }
      }
    }
    getRender().update();

    EventManager.INSTANCE.javaOneShotListener(
        InitialChunksOfWorldLoadedEvent.class,
        event -> {
          if (worldTicker.isStarted()) {
            throw new IllegalStateException("World has already been started");
          }
          worldTicker.start();

          if (this instanceof SinglePlayerWorld spWorld
              && getEngine().getEntitiesFor(AshleyKt.getPlayerFamily()).size() == 0) {
            createSPPlayerEntity(
                getEngine(), spWorld, spawn.getX(), spawn.getY(), 0f, 0f, "Player", null);
          }
          EventManager.INSTANCE.javaDispatchEvent(new WorldLoadedEvent(this));
        });

    Main.inst()
        .getScheduler()
        .executeAsync(
            () -> {
              Location chunkSpawn = CoordUtil.worldToChunk(spawn);
              loadChunk(chunkSpawn.getX(), chunkSpawn.getY());

              for (Location location : getRender().getChunkLocationsInView()) {
                loadChunk(location.getX(), location.getY());
              }

              Main.inst()
                  .getScheduler()
                  .executeSync(
                      () ->
                          EventManager.INSTANCE.javaDispatchEvent(
                              new InitialChunksOfWorldLoadedEvent(this)));
            });
  }

  public void loadFromProtoWorld(@NotNull ProtoWorld.WorldOrBuilder protoWorld) {
    spawn = Location.Companion.fromVector2i(protoWorld.getSpawn());
    worldTime.setTimeScale(protoWorld.getTimeScale());
    worldTime.setTime(protoWorld.getTime());
    synchronized (chunkColumns) {
      for (ProtoWorld.ChunkColumn protoCC : protoWorld.getChunkColumnsList()) {
        ChunkColumn chunkColumn = ChunkColumnImpl.Companion.fromProtobuf(this, protoCC);
        int chunkX = protoCC.getChunkX();
        chunkColumns.put(chunkX, chunkColumn);
      }
    }
  }

  public void save() {
    if (transientWorld) {
      return;
    }
    FileHandle worldFolder = getWorldFolder();
    if (worldFolder == null) {
      return;
    }

    chunksLock.writeLock().lock();
    try {
      for (Chunk chunk : chunks.values()) {
        chunkLoader.save(chunk);
      }

      //      for (Player player : players.values()) {
      //        saveServerPlayer(player);
      //      }

      var builder = toProtobuf();

      var worldInfoFile = worldFolder.child(WorldLoader.WORLD_INFO_PATH);
      if (worldInfoFile.exists()) {
        worldInfoFile.moveTo(worldFolder.child(WorldLoader.WORLD_INFO_PATH + ".old"));
      }
      worldInfoFile.writeBytes(builder.toByteArray(), false);
    } finally {
      chunksLock.writeLock().unlock();
    }
  }

  @NotNull
  public ProtoWorld.World toProtobuf() {
    var builder = ProtoWorld.World.newBuilder();
    builder.setName(name);
    builder.setSeed(seed);
    builder.setTime(worldTime.getTime());
    builder.setTimeScale(worldTime.getTimeScale());
    builder.setSpawn(spawn.toVector2i());
    builder.setGenerator(getGeneratorType());
    synchronized (chunkColumns) {
      for (ChunkColumn chunkColumn : chunkColumns.values()) {
        builder.addChunkColumns(chunkColumn.toProtobuf());
      }
    }

    //    if (Main.isSingleplayer()) {
    //      Player player = ClientMain.inst().getPlayer();
    //      if (player != null) {
    //        builder.setPlayer(player.save());
    //      }
    //    }
    return builder.build();
  }

  /**
   * @return The current folder of the world or {@code null} if no disk should be used
   */
  @Nullable
  public FileHandle getWorldFolder() {
    if (transientWorld) {
      return null;
    }
    if (worldFile == null) {
      worldFile = WorldLoader.getWorldFolder(uuid);
    }
    return worldFile;
  }

  private ProtoWorld.World.Generator getGeneratorType() {
    ChunkGenerator generator = chunkLoader.getGenerator();
    if (generator instanceof PerlinChunkGenerator) {
      return PERLIN;
    } else if (generator instanceof FlatChunkGenerator) {
      return FLAT;
    } else if (generator instanceof EmptyChunkGenerator) {
      return EMPTY;
    } else {
      return UNRECOGNIZED;
    }
  }

  @NotNull
  public ChunkColumn getChunkColumn(int chunkX) {
    synchronized (chunkColumns) {
      var column = chunkColumns.get(chunkX);
      if (column == null) {
        ChunkColumn newCol = new ChunkColumnImpl(this, chunkX, null, null);
        chunkColumns.put(chunkX, newCol);
        return newCol;
      }
      return column;
    }
  }

  /**
   * @param features What kind of top block to return
   * @return The block (including Air!) at the given local x, if `null` the chunk failed to load.
   * @see no.elg.infiniteBootleg.world.ChunkColumn.Companion.FeatureFlag
   */
  @Nullable
  public Block getTopBlock(int worldX, int features) {
    return getChunkColumn(CoordUtil.worldToChunk(worldX))
        .topBlock(CoordUtil.chunkOffset(worldX), features);
  }

  /**
   * @param worldX The block column to query for the worldX for
   * @param features What kind of top block to return
   * @return The worldY coordinate of the top block of the given worldX
   * @see no.elg.infiniteBootleg.world.ChunkColumn.Companion.FeatureFlag
   */
  public int getTopBlockWorldY(int worldX, int features) {
    return getChunkColumn(CoordUtil.worldToChunk(worldX))
        .topBlockHeight(CoordUtil.chunkOffset(worldX), features);
  }

  @Nullable
  public Chunk getChunkFromWorld(int worldX, int worldY, boolean load) {
    int chunkX = CoordUtil.worldToChunk(worldX);
    int chunkY = CoordUtil.worldToChunk(worldY);
    return getChunk(chunkX, chunkY, load);
  }

  public void updateChunk(@NotNull Chunk chunk) {
    Preconditions.checkState(chunk.isValid());
    chunksLock.writeLock().lock();

    @Nullable Chunk old;
    try {
      old = chunks.put(chunk.getCompactLocation(), chunk);
    } finally {
      chunksLock.writeLock().unlock();
    }
    if (old != null) {
      old.dispose();
    }
  }

  @Nullable
  public Chunk getChunk(@NotNull Location chunkLoc) {
    return getChunk(chunkLoc.toCompactLocation(), true);
  }

  @Nullable
  public Chunk getChunk(int chunkX, int chunkY, boolean load) {
    return getChunk(CoordUtil.compactLoc(chunkX, chunkY), load);
  }

  @Nullable
  public Chunk getChunk(long chunkLoc) {
    return getChunk(chunkLoc, true);
  }

  @Nullable
  public Chunk getChunk(long chunkLoc, boolean load) {
    // This is a long lock, it must appear to be an atomic operation though
    @Nullable Chunk readChunk = null;
    boolean acquiredLock = false;
    try {
      acquiredLock =
          chunksLock.readLock().tryLock(TRY_LOCK_CHUNKS_DURATION_MS, TimeUnit.MILLISECONDS);
      if (acquiredLock) {
        readChunk = chunks.get(chunkLoc);
      }
    } catch (InterruptedException ignore) {
    } finally {
      if (acquiredLock) {
        chunksLock.readLock().unlock();
      }
    }

    if (!acquiredLock) {
      Main.logger()
          .warn(
              "World",
              "Failed to acquire chunks read lock in " + TRY_LOCK_CHUNKS_DURATION_MS + " ms");
      return null;
    }

    if (readChunk == null || readChunk.isInvalid()) {
      if (!load) {
        return null;
      }
      return loadChunk(chunkLoc, true);
    }
    return readChunk;
  }

  /**
   * Load a chunk into memory, either from disk or generate the chunk from its position.
   *
   * <p>A chunk will not be loaded if there exists a valid chunk at the chunk position.
   *
   * @param chunkX The x-coordinate of the chunk to load (in Chunk coordinate-view)
   * @param chunkY The y-coordinate of the chunk to load (in Chunk coordinate-view)
   * @return The loaded chunk
   */
  @Nullable
  public Chunk loadChunk(int chunkX, int chunkY) {
    return loadChunk(CoordUtil.compactLoc(chunkX, chunkY), true);
  }

  /**
   * Load a chunk into memory, either from disk or generate the chunk from its position
   *
   * @param chunkLoc The location of the chunk (in Chunk coordinate-view)
   * @param returnIfLoaded Whether to check if there is a loaded and valid chunk at the given
   *     `chunkLocation` and thus not reload the chunk
   * @return The loaded chunk
   */
  @Nullable
  public Chunk loadChunk(long chunkLoc, boolean returnIfLoaded) {
    if (getWorldTicker().isPaused()) {
      Main.logger().debug("World", "Ticker paused will not load chunk");
      return null;
    }
    @Nullable Chunk old = null;
    chunksLock.writeLock().lock();
    try {
      if (returnIfLoaded) {
        var current = chunks.get(chunkLoc);
        if (current != null) {
          if (current.isValid()) {
            return current;
          }
          if (current.isNotDisposed()) {
            // If the current chunk is not valid, but not disposed either, so it should be loading
            // We don't want to load a new chunk when the current one is finishing its loading
            return null;
          }
        }
      }

      //      Main.logger().log("Loading world chunk at " + CoordUtil.stringifyCompactLoc(chunkLoc)
      // + "\n" + DebugUtilsKt.stacktrace());
      Chunk loadedChunk = chunkLoader.load(chunkLoc);
      if (loadedChunk == null) {
        // If we failed to load the old chunk assume the loaded chunk (if any) is corrupt, out of
        // date, and the loading should be re-tried
        old = chunks.remove(chunkLoc);
        return null;
      } else {
        Preconditions.checkState(loadedChunk.isValid());
        old = chunks.put(chunkLoc, loadedChunk);
        EventManager.INSTANCE.javaDispatchEvent(new ChunkLoadedEvent(loadedChunk));
        return loadedChunk;
      }
    } finally {
      chunksLock.writeLock().unlock();
      if (old != null) {
        old.dispose();
      }
    }
  }

  /**
   * Set a block at a given location and update the textures
   *
   * @param worldLoc The location in world coordinates
   * @param material The new material to at given location
   * @see Chunk#setBlock(int, int, Material, boolean)
   */
  public Block setBlock(@NotNull Location worldLoc, @Nullable Material material) {
    return setBlock(worldLoc, material, true);
  }

  /**
   * Set a block at a given location
   *
   * @param worldLoc The location in world coordinates
   * @param material The new material to at given location
   * @param update If the texture of the corresponding chunk should be updated
   * @see Chunk#setBlock(int, int, Material, boolean)
   */
  public Block setBlock(@NotNull Location worldLoc, @Nullable Material material, boolean update) {
    return setBlock(worldLoc.getX(), worldLoc.getY(), material, update);
  }

  /**
   * Set a block at a given location
   *
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   * @param material The new material to at given location
   * @param updateTexture If the texture of the corresponding chunk should be updated
   * @see Chunk#setBlock(int, int, Material, boolean)
   */
  @Nullable
  public Block setBlock(
      int worldX, int worldY, @Nullable Material material, boolean updateTexture) {
    return setBlock(worldX, worldY, material, updateTexture, false);
  }

  public Block setBlock(
      int worldX,
      int worldY,
      @Nullable Material material,
      boolean updateTexture,
      boolean prioritize) {
    int chunkX = CoordUtil.worldToChunk(worldX);
    int chunkY = CoordUtil.worldToChunk(worldY);

    int localX = worldX - chunkX * Chunk.CHUNK_SIZE;
    int localY = worldY - chunkY * Chunk.CHUNK_SIZE;

    Chunk chunk = getChunk(chunkX, chunkY, true);
    if (chunk != null) {
      return chunk.setBlock(localX, localY, material, updateTexture, prioritize);
    }
    return null;
  }

  /**
   * Set a block at a given location and update the textures
   *
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   * @param material The new material to at given location
   * @see Chunk#setBlock(int, int, Material, boolean)
   */
  public Block setBlock(int worldX, int worldY, @Nullable Material material) {
    return setBlock(worldX, worldY, material, true);
  }

  /**
   * Set a block at a given location
   *
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   * @param block The block at the given location
   * @param update If the texture of the corresponding chunk should be updated
   */
  public void setBlock(int worldX, int worldY, @Nullable Block block, boolean update) {

    int chunkX = CoordUtil.worldToChunk(worldX);
    int chunkY = CoordUtil.worldToChunk(worldY);

    int localX = CoordUtil.chunkOffset(worldX);
    int localY = CoordUtil.chunkOffset(worldY);

    Chunk chunk = getChunk(chunkX, chunkY, true);
    if (chunk != null) {
      chunk.setBlock(localX, localY, block, update);
    }
  }

  public void setBlock(
      int worldX, int worldY, @Nullable ProtoWorld.Block protoBlock, boolean sendUpdatePacket) {
    int chunkX = CoordUtil.worldToChunk(worldX);
    int chunkY = CoordUtil.worldToChunk(worldY);

    int localX = CoordUtil.chunkOffset(worldX);
    int localY = CoordUtil.chunkOffset(worldY);

    Chunk chunk = getChunk(chunkX, chunkY, true);
    if (chunk != null) {
      var block = Block.fromProto(this, chunk, localX, localY, protoBlock);
      chunk.setBlock(localX, localY, block, true, false, sendUpdatePacket);
    }
  }

  /**
   * Remove anything that is at the given location be it a {@link Block} or {@link MaterialEntity}
   *
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   * @param update If the texture of the corresponding chunk should be updated
   */
  public void remove(int worldX, int worldY, boolean update) {
    int chunkX = CoordUtil.worldToChunk(worldX);
    int chunkY = CoordUtil.worldToChunk(worldY);

    int localX = CoordUtil.chunkOffset(worldX);
    int localY = CoordUtil.chunkOffset(worldY);

    Chunk chunk = getChunk(chunkX, chunkY, true);
    if (chunk != null) {
      chunk.setBlock(localX, localY, (Block) null, update);
    }
  }

  public void removeBlocks(Iterable<? extends @NotNull Block> blocks, boolean prioritize) {
    var blockChunks = new ObjectSet<Chunk>();
    for (Block block : blocks) {
      block.destroy(false);
      blockChunks.add(block.getChunk());
    }
    for (Chunk chunk : blockChunks) {
      chunk.updateTexture(prioritize);
    }
    // only update once all the correct blocks have been removed
    for (Block block : blocks) {
      updateBlocksAround(block.getWorldX(), block.getWorldY());
    }
  }

  public Array<Entity> getEntities(float worldX, float worldY) {
    Array<Entity> foundEntities = new Array<>(false, 4);
    for (var entity : engine.getEntitiesFor(AshleyKt.getBasicEntityFamily())) {
      var pos = entity.getComponent(PositionComponent.class);
      var size = entity.getComponent(Box2DBodyComponent.class);
      if (Util.isBetween(
              MathUtils.floor((float) (pos.getX() - size.getHalfBox2dWidth())),
              worldX,
              MathUtils.ceil((float) (pos.getX() + size.getHalfBox2dWidth())))
          && //
          Util.isBetween(
              MathUtils.floor((float) (pos.getY() - size.getHalfBox2dHeight())),
              worldY,
              MathUtils.ceil((float) (pos.getY() + size.getHalfBox2dHeight())))) {

        foundEntities.add(entity);
      }
    }
    return foundEntities;
  }

  /**
   * Check if a given location in the world is {@link Material#AIR} (or internally, doesn't exists)
   * this is faster than a standard {@code getBlock(worldX, worldY).getMaterial == Material.AIR} as
   * the {@link #getRawBlock(int, int, boolean)} method might createBlock and store a new air block
   * at the given location
   *
   * <p><b>note</b> this does not if there are entities at this location
   *
   * @param worldLoc The world location to check
   * @return If the block at the given location is air.
   */
  public boolean isAirBlock(@NotNull Location worldLoc) {
    return isAirBlock(worldLoc.getX(), worldLoc.getY());
  }

  public boolean isAirBlock(long compactWorldLoc) {
    return isAirBlock(
        CoordUtil.decompactLocX(compactWorldLoc), CoordUtil.decompactLocY(compactWorldLoc));
  }

  /**
   * Check if a given location in the world is {@link Material#AIR} (or internally, does not exist)
   * this is faster than a standard {@code getBlock(worldX, worldY).getMaterial == Material.AIR} as
   * the {@link #getRawBlock(int, int, boolean)} method might create a Block and store a new air
   * block at the given location.
   *
   * <p>If the chunk at the given coordinates isn't loaded yet this method return `false` to prevent
   * teleportation and other actions that depend on an empty space.
   *
   * <p><b>note</b> this does not if there are entities at this location
   *
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   * @return If the block at the given location is air.
   */
  public boolean isAirBlock(int worldX, int worldY) {
    int chunkX = CoordUtil.worldToChunk(worldX);
    int chunkY = CoordUtil.worldToChunk(worldY);

    int localX = worldX - chunkX * Chunk.CHUNK_SIZE;
    int localY = worldY - chunkY * Chunk.CHUNK_SIZE;

    Chunk chunk = getChunk(chunkX, chunkY, false);
    if (chunk == null) {
      // What should we return here? we don't really know as it does not exist.
      // Return false to prevent teleportation and other actions that depend on an empty space.
      return false;
    }

    Block b = chunk.getBlocks()[localX][localY];
    return b == null || b.getMaterial() == Material.AIR;
  }

  public boolean canPassThrough(int worldX, int worldY) {
    int chunkX = CoordUtil.worldToChunk(worldX);
    int chunkY = CoordUtil.worldToChunk(worldY);

    int localX = worldX - chunkX * Chunk.CHUNK_SIZE;
    int localY = worldY - chunkY * Chunk.CHUNK_SIZE;

    Chunk chunk = getChunk(chunkX, chunkY, true);
    if (chunk == null) {
      // What should we return here? we don't really know as it does not exist.
      // Return false to prevent teleportation and other actions that depend on an empty space.
      return false;
    }

    Block b = chunk.getBlocks()[localX][localY];
    return b == null || !b.getMaterial().isSolid();
  }

  /**
   * Update blocks around the given location
   *
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   */
  public void updateBlocksAround(int worldX, int worldY) {

    Array<@NotNull Block> blocksAABB =
        getBlocksAABBFromCenter(
            worldX + HALF_BLOCK_SIZE,
            worldY + HALF_BLOCK_SIZE,
            LIGHT_SOURCE_LOOK_BLOCKS,
            LIGHT_SOURCE_LOOK_BLOCKS,
            true,
            false,
            false,
            null,
            null);

    LongMap<Chunk> chunks = new LongMap<>();
    for (Block block : blocksAABB) {
      chunks.put(block.getChunk().getCompactLocation(), block.getChunk());
      if (block instanceof TickingBlock tickingBlock) {
        tickingBlock.enableTick();
      }
    }
    for (Chunk chunk : chunks.values()) {
      chunk.dirty();
    }
  }

  /**
   * @param compactWorldLoc The coordinates from world view in a compact form
   * @param load Load the chunk at the coordinates
   * @return The block at the given x and y
   */
  @Nullable
  public Block getRawBlock(long compactWorldLoc, boolean load) {
    int worldY = CoordUtil.decompactLocY(compactWorldLoc);
    int worldX = CoordUtil.decompactLocX(compactWorldLoc);
    return getRawBlock(worldX, worldY, load);
  }

  /**
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   * @param load Load the chunk at the coordinates
   * @return The block at the given x and y (or null if air block)
   */
  @Nullable
  public Block getRawBlock(int worldX, int worldY, boolean load) {
    int chunkX = CoordUtil.worldToChunk(worldX);
    int chunkY = CoordUtil.worldToChunk(worldY);

    int localX = worldX - chunkX * Chunk.CHUNK_SIZE;
    int localY = worldY - chunkY * Chunk.CHUNK_SIZE;

    Chunk chunk = getChunk(chunkX, chunkY, load);
    if (chunk == null) {
      return null;
    }
    return chunk.getRawBlock(localX, localY);
  }

  /**
   * Note an air block will be created if the chunk is loaded and there is no other block at the
   * given location
   *
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   * @return The block at the given x and y
   */
  @Nullable
  public Block getBlock(int worldX, int worldY) {
    return getBlock(worldX, worldY, true);
  }

  @Nullable
  public BlockLight getBlockLight(int worldX, int worldY, boolean loadChunk) {
    var chunk = getChunkFromWorld(worldX, worldY, loadChunk);
    if (chunk == null) {
      return null;
    }
    return chunk.getBlockLight(CoordUtil.chunkOffset(worldX), CoordUtil.chunkOffset(worldY));
  }

  /**
   * Note an air block will be created if the chunk is loaded and there is no other block at the
   * given location
   *
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   * @param loadChunk
   * @return The block at the given x and y
   */
  @Nullable
  public Block getBlock(int worldX, int worldY, boolean loadChunk) {
    int chunkX = CoordUtil.worldToChunk(worldX);
    int chunkY = CoordUtil.worldToChunk(worldY);

    int localX = worldX - chunkX * Chunk.CHUNK_SIZE;
    int localY = worldY - chunkY * Chunk.CHUNK_SIZE;

    Chunk chunk = getChunk(chunkX, chunkY, loadChunk);
    if (chunk == null) {
      return null;
    }
    return chunk.getBlock(localX, localY);
  }

  /**
   * Use {@link #isChunkLoaded(int, int)} or {@link #isChunkLoaded(long)} if possible
   *
   * @param chunkLoc Chunk location in chunk coordinates
   * @return If the given chunk is loaded in memory
   */
  public boolean isChunkLoaded(@NotNull Location chunkLoc) {
    return isChunkLoaded(chunkLoc.toCompactLocation());
  }

  /**
   * @return If the given chunk is loaded in memory
   */
  public boolean isChunkLoaded(int chunkX, int chunkY) {
    return isChunkLoaded(CoordUtil.compactLoc(chunkX, chunkY));
  }

  public boolean isChunkLoaded(long compactedChunkLoc) {
    Chunk chunk;
    chunksLock.readLock().lock();
    try {
      chunk = chunks.get(compactedChunkLoc);
    } finally {
      chunksLock.readLock().unlock();
    }
    return chunk != null && chunk.isNotDisposed();
  }

  /**
   * Unload and save all chunks in this world.
   *
   * <p>Must be called on main thread!
   *
   * @param force If the chunks will be forced to unload
   */
  public void reload(boolean force) {
    Main.inst()
        .getScheduler()
        .executeSync(
            () -> {
              // Reload on render thread to make sure it does not try to load chunks while we're
              // waiting
              var wasNotPaused = !worldTicker.isPaused();
              if (wasNotPaused) {
                worldTicker.pause();
              }
              Main.inst().getScheduler().waitForTasks();

              // remove all entities to speed up unloading
              //          for (Entity entity : getEntities()) {
              //            removeEntity(entity);
              //          }
              //          if (!entities.isEmpty() || !players.isEmpty()) {
              //            throw new IllegalStateException("Failed to clear entities during
              // reload");
              //          }
              // ok to include unloaded chunks as they will not cause an error when unloading again
              chunksLock.writeLock().lock();
              try {
                for (@Nullable Chunk chunk : chunks.values()) {
                  if (chunk != null && !unloadChunk(chunk, true, false)) {
                    Main.logger()
                        .warn("Failed to unload chunk " + CoordUtil.stringifyCompactLoc(chunk));
                  }
                }

                var loadedChunks = chunks.size;
                if (loadedChunks != 0) {
                  Main.logger()
                      .warn(
                          "Failed to clear chunks during reload, there are "
                              + loadedChunks
                              + " loaded chunks.");
                }
              } finally {
                chunksLock.writeLock().unlock();
              }
              synchronized (BOX2D_LOCK) {
                var bodies =
                    new Array<@NotNull Body>(false, worldBody.getBox2dWorld().getBodyCount());
                worldBody.getBox2dWorld().getBodies(bodies);
                if (!bodies.isEmpty()) {
                  Main.logger().error("BOX2D", "There existed dangling bodies after reload!");
                }
                for (Body body : bodies) {
                  worldBody.destroyBody(body);
                }
              }

              initialize();

              if (wasNotPaused) {
                worldTicker.resume();
              }
              Main.logger().log("World", "World reloaded last save");
            });
  }

  /**
   * @return All currently loaded chunks
   */
  @NotNull
  public Array<@NotNull Chunk> getLoadedChunks() {
    chunksLock.readLock().lock();
    try {
      var loadedChunks = new Array<@NotNull Chunk>(true, chunks.size, Chunk.class);
      for (Chunk chunk : chunks.values()) {
        if (chunk != null && chunk.isNotDisposed()) {
          loadedChunks.add(chunk);
        }
      }
      return loadedChunks;
    } finally {
      chunksLock.readLock().unlock();
    }
  }

  public @Nullable Chunk getLoadedChunk(long compactChunkLoc) {
    chunksLock.readLock().lock();
    try {
      return chunks.get(compactChunkLoc);
    } finally {
      chunksLock.readLock().unlock();
    }
  }

  /**
   * Unload the given chunks and save it to disk
   *
   * @param chunk The chunk to unload
   */
  public void unloadChunk(@Nullable Chunk chunk) {
    unloadChunk(chunk, false, true);
  }

  /**
   * Unload the given chunks and save it to disk
   *
   * @param chunk The chunk to unload
   * @param force If the chunk will be forced to unload
   * @param save If the chunk will be saved
   * @return If the unloading was successful
   */
  public boolean unloadChunk(@Nullable Chunk chunk, boolean force, boolean save) {
    if (chunk != null && chunk.isNotDisposed() && (force || chunk.isAllowingUnloading())) {
      if (chunk.getWorld() != this) {
        Main.logger().warn("Tried to unload chunk from different world");
        return false;
      }
      Chunk removedChunk;
      chunksLock.writeLock().lock();
      try {
        if (save) {
          chunkLoader.save(chunk);
        }

        //        for (Entity entity : chunk.getEntities()) {
        //          removeEntity(entity, CHUNK_UNLOADED);
        //        }
        removedChunk = chunks.remove(chunk.getCompactLocation());
      } finally {
        chunksLock.writeLock().unlock();
      }

      chunk.dispose();
      if (removedChunk != null && chunk != removedChunk) {
        Main.logger()
            .warn(
                "Removed unloaded chunk "
                    + CoordUtil.stringifyCompactLoc(chunk)
                    + " was different from chunk in list of loaded chunks: "
                    + CoordUtil.stringifyCompactLoc(removedChunk)
                    + "\n"
                    + DebugUtilsKt.stacktrace());
        removedChunk.dispose();
      }
      return true;
    }
    return false;
  }

  /**
   * @param worldLoc The world location of this chunk
   * @return The chunk at the given world location
   */
  @Nullable
  public Chunk getChunkFromWorld(@NotNull Location worldLoc) {
    return getChunk(CoordUtil.worldToChunk(worldLoc));
  }

  public boolean containsEntity(@NotNull String uuid) {
    return getEntity(uuid) != null;
  }

  @Nullable
  public Entity getEntity(@NotNull String uuid) {
    ImmutableArray<Entity> entitiesFor =
        getEngine().getEntitiesFor(AshleyKt.getBasicEntityFamily());
    for (Entity entity : entitiesFor) {
      if (entity.getComponent(IdComponent.class).component1().equals(uuid)) {
        return entity;
      }
    }
    return null;
  }

  /**
   * Add the given entity to entities in the world. <b>NOTE</b> this is NOT automatically done when
   * creating a new entity instance.
   *
   * @param entity The entity to add
   */
  public void addEntity(@NotNull Entity entity) {
    addEntity(entity, true);
  }

  /**
   * Add the given entity to entities in the world. <b>NOTE</b> this is NOT automatically done when
   * creating a new entity instance.
   *
   * @param entity The entity to add
   * @param loadChunk Whether to load the chunk the entity will exist in
   */
  public void addEntity(@NotNull Entity entity, boolean loadChunk) {
    Main.inst().getScheduler().executeAsync(() -> syncAddEntity(entity, loadChunk));
  }

  /**
   * Add the given entity to entities in the world. <b>NOTE</b> this is NOT automatically done when
   * creating a new entity instance.
   *
   * @param entity The entity to add
   * @param loadChunk Whether to load the chunk the entity will exist in
   */
  protected void syncAddEntity(@NotNull Entity entity, boolean loadChunk) {
    //    if (engine.addEntity();)
    //    if (entities.containsValue(entity)) {
    //      Main.logger()
    //        .error(
    //          "World",
    //          "Tried to add entity twice to world "
    //            + entity.simpleName()
    //            + " "
    //            + entity.hudDebug());
    //      return;
    //    }
    //    if (containsEntity(entity.getUuid())) {
    //      Main.logger()
    //        .error(
    //          "World",
    //          "Tried to add duplicate entity to world "
    //            + entity.simpleName()
    //            + " "
    //            + entity.hudDebug());
    //      removeEntity(entity);
    //      return;
    //    }

    //    getEngine().getEntitiesFor(AshleyKt.getBasicEntityFamily())

    //    if (IdComponent.Companion.getMapper().has(entity))
    //
    //      if (loadChunk) {
    //        // Load chunk of entity
    //        var chunk =
    //          getChunk(
    //            CoordUtil.worldToChunk(entity.getBlockX()),
    //            CoordUtil.worldToChunk(entity.getBlockY()),
    //            true);
    //        if (chunk == null) {
    //          // Failed to load chunk, remove entity
    //          Main.logger()
    //            .error(
    //              "World",
    //              "Failed to add entity to world, as its spawning chunk could not be loaded");
    //          removeEntity(entity);
    //          return;
    //        }
    //      }

    //    entities.put(entity.getUuid(), entity);
    //    if (entity instanceof Player player) {
    //      players.put(player.getUuid(), player);
    //      saveServerPlayer(player);
    //    }
  }

  /**
   * Remove and disposes the given entity.
   *
   * <p>Even if the given entity is not a part of this world, it will be disposed
   *
   * @param entity The entity to remove
   * @throws IllegalArgumentException if the given entity is not part of this world
   */
  public void removeEntity(@NotNull Entity entity) {
    removeEntity(entity, UNKNOWN_REASON);
  }

  public void removeEntity(
      @NotNull Entity entity, @NotNull Packets.DespawnEntity.DespawnReason reason) {
    ServerHelperKt.despawnEntity(entity, reason);
    engine.removeEntity(entity);
  }

  @Nullable
  public Entity getPlayer(@NotNull String uuid) {
    for (Entity entity : engine.getEntitiesFor(AshleyKt.getPlayerFamily())) {
      if (IdComponent.Companion.getIdComponent(entity).getId().equals(uuid)) {
        return entity;
      }
    }
    return null;
  }

  public boolean hasPlayer(@NotNull String uuid) {
    return getPlayer(uuid) != null;
  }

  /**
   * @param worldX X center (center of each block
   * @param worldY Y center
   * @param radius Radius to be equal or less from center
   * @return Set of blocks within the given radius
   */
  @NotNull
  public ObjectSet<@NotNull Block> getBlocksWithin(float worldX, float worldY, float radius) {
    Preconditions.checkArgument(radius >= 0, "Radius should be a non-negative number");
    ObjectSet<Block> blocks = new ObjectSet<>();
    float radiusSquare = radius * radius;
    for (long compact : getLocationsAABB(worldX, worldY, radius, radius).items) {
      int blockWorldX = CoordUtil.decompactLocX(compact);
      int blockWorldY = CoordUtil.decompactLocY(compact);
      if (abs(
              Vector2.dst2(
                  worldX, worldY, blockWorldX + HALF_BLOCK_SIZE, blockWorldY + HALF_BLOCK_SIZE))
          <= radiusSquare) {
        Block block = getBlock(blockWorldX, blockWorldY);
        if (block == null) {
          continue;
        }
        blocks.add(block);
      }
    }
    return blocks;
  }

  /**
   * @param centerWorldX Center world X coordinate
   * @param centerWorldY Center world Y coordinate
   * @param width How far to go on either side of the x-axis.
   * @param height How far to go on either side of the y-axis.
   * @param raw If non-existing blocks (i.e., air) should be created to be included
   * @param loadChunk Whether to load the chunks the blocks exist in. If false no blocks from
   *     unloaded chunks will be included
   * @return An Axis-Aligned Bounding Box of blocks
   */
  @NotNull
  public Array<@NotNull Block> getBlocksAABBFromCenter(
      float centerWorldX,
      float centerWorldY,
      float width,
      float height,
      boolean raw,
      boolean loadChunk,
      boolean includeAir,
      @Nullable Function0<Boolean> cancel,
      @Nullable Function1<Block, Boolean> filter) {

    Preconditions.checkArgument(width >= 0, "Width must be >= 0, was " + width);
    Preconditions.checkArgument(height >= 0, "Height must be >= 0, was " + height);
    float worldX = centerWorldX - width;
    float worldY = centerWorldY - height;
    return getBlocksAABB(
        worldX, worldY, width * 2f, height * 2f, raw, loadChunk, includeAir, cancel, filter);
  }

  /**
   * @param worldX Lower world X coordinate
   * @param worldY Lower world Y coordinate
   * @param offsetX Offset from the world X coordinate
   * @param offsetY Offset from the world Y coordinate
   * @param raw If non-existing blocks (i.e., air) should be created to be included
   * @param loadChunk Whether to load the chunks the blocks exist in. If false no blocks from
   *     unloaded chunks will be included
   * @param includeAir Whether air should be included
   * @param cancel Allows for early termination of this method
   * @return An Axis-Aligned Bounding Box of blocks
   */
  @NotNull
  public Array<@NotNull Block> getBlocksAABB(
      float worldX,
      float worldY,
      float offsetX,
      float offsetY,
      boolean raw,
      boolean loadChunk,
      boolean includeAir,
      @Nullable Function0<Boolean> cancel,
      @Nullable Function1<Block, Boolean> filter) {
    boolean effectiveRaw;
    if (!raw && !includeAir) {
      Main.logger()
          .warn(
              "getBlocksAABB",
              "Will not include air AND air blocks will be created! (raw: false, includeAir:"
                  + " false)");
      effectiveRaw = true;
    } else {
      effectiveRaw = raw;
    }
    Preconditions.checkArgument(offsetX >= 0, "offsetX must be >= 0, was " + offsetX);
    Preconditions.checkArgument(offsetY >= 0, "offsetY must be >= 0, was " + offsetY);

    int capacity = MathUtils.floorPositive(abs(offsetX)) * MathUtils.floorPositive(abs(offsetY));
    Array<Block> blocks = new Array<>(true, capacity);
    int x = MathUtils.floor(worldX);
    int startY = MathUtils.floor(worldY);
    float maxX = worldX + offsetX;
    float maxY = worldY + offsetY;
    LongMap<Chunk> chunks = new LongMap<>();
    for (; x <= maxX; x++) {
      for (int y = startY; y <= maxY; y++) {
        if (cancel != null && cancel.invoke()) {
          return blocks;
        }
        var chunkPos = CoordUtil.compactLoc(CoordUtil.worldToChunk(x), CoordUtil.worldToChunk(y));
        var chunk = chunks.get(chunkPos);
        if (chunk == null || chunk.isInvalid()) {
          chunk = getChunk(chunkPos, loadChunk);
          if (chunk == null) {
            continue;
          }
          chunks.put(chunkPos, chunk);
        }

        int localX = CoordUtil.chunkOffset(x);
        int localY = CoordUtil.chunkOffset(y);
        Block b = effectiveRaw ? chunk.getRawBlock(localX, localY) : chunk.getBlock(localX, localY);
        if (b == null) continue;
        if ((includeAir || !ExtraKt.isAir(b)) && (filter == null || filter.invoke(b))) {
          blocks.add(b);
        }
      }
    }
    return blocks;
  }

  @NotNull
  public static LongArray getLocationsAABB(
      float worldX, float worldY, float offsetX, float offsetY) {
    int capacity = MathUtils.floorPositive(abs(offsetX)) * MathUtils.floorPositive(abs(offsetY));
    LongArray blocks = new LongArray(true, capacity);
    int x = MathUtils.floor(worldX - offsetX);
    float maxX = worldX + offsetX;
    float maxY = worldY + offsetY;
    for (; x <= maxX; x++) {
      for (int y = MathUtils.floor(worldY - offsetY); y <= maxY; y++) {
        blocks.add(CoordUtil.compactLoc(x, y));
      }
    }
    return blocks;
  }

  /**
   * @param worldX The x coordinate in world view
   * @param worldY The y coordinate in world view
   * @return The first entity found within the given coordinates
   */
  @Nullable
  public Entity getEntity(float worldX, float worldY) {
    //    for (Entity entity : entities.values()) {
    //      Vector2 pos = entity.getPosition();
    //      if (Util.isBetween(
    //        pos.getX() - entity.getHalfBox2dWidth(), worldX, pos.x + entity.getHalfBox2dWidth())
    //        && //
    //        Util.isBetween(
    //          pos.y - entity.getHalfBox2dHeight(), worldY, pos.y + entity.getHalfBox2dHeight())) {
    //        return entity;
    //      }
    //    }
    return null;
  }

  /**
   * @param worldX The x coordinate in world view
   * @param worldY The y coordinate in world view
   * @return The material at the given location
   */
  @NotNull
  public Material getMaterial(int worldX, int worldY) {
    Block block = getRawBlock(worldX, worldY, true);
    if (block != null) {
      return block.getMaterial();
    }

    //    for (Entity entity : getEntities(worldX, worldY)) {
    //      if (entity instanceof MaterialEntity materialEntity) {
    //        return materialEntity.getMaterial();
    //      }
    //    }
    return Material.AIR;
  }

  /** Alias to {@code WorldBody#postBox2dRunnable} */
  public void postBox2dRunnable(Runnable runnable) {
    worldBody.postBox2dRunnable(runnable);
  }

  /**
   * Must be accessed under {@link #chunksLock}
   *
   * @return Backing map of chunks
   */
  @GuardedBy("chunksLock")
  public @NotNull LongMap<@Nullable Chunk> getChunks() {
    return chunks;
  }

  /**
   * @return The random seed of this world
   */
  public long getSeed() {
    return seed;
  }

  /**
   * @return The name of the world
   */
  public @NotNull String getName() {
    return name;
  }

  /**
   * @return Unique identification of this world
   */
  public @NotNull String getUuid() {
    return uuid;
  }

  /**
   * @return The current world tick
   */
  public long getTick() {
    return worldTicker.getTickId();
  }

  @NotNull
  public abstract WorldRender getRender();

  @NotNull
  public Ticker getWorldTicker() {
    return worldTicker;
  }

  @NotNull
  public Ticker getBox2DTicker() {
    return worldTicker.box2DTicker.getTicker();
  }

  //  /**
  //   * @return the current entities
  //   */
  //  public @NotNull Collection<Entity> getEntities() {
  //    return new HashSet<>(entities.values());
  //  }
  //
  //  public @NotNull Collection<Player> getPlayers() {
  //    return new HashSet<>(players.values());
  //  }
  //
  //  public boolean hasPlayer(@NotNull UUID uuid) {
  //    return players.containsKey(uuid);
  //  }
  //
  //  @Nullable
  //  public Player getPlayer(@Nullable UUID uuid) {
  //    return players.get(uuid);
  //  }

  public @NotNull ChunkLoader getChunkLoader() {
    return chunkLoader;
  }

  @NotNull
  public WorldBody getWorldBody() {
    return worldBody;
  }

  public @NotNull WorldTime getWorldTime() {
    return worldTime;
  }

  public Location getSpawn() {
    return spawn;
  }

  public void setSpawn(Location spawn) {
    this.spawn = spawn;
  }

  @NotNull
  public Engine getEngine() {
    return engine;
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
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    World world = (World) o;
    return uuid.equals(world.uuid);
  }

  @Override
  public void resize(int width, int height) {}

  @Override
  public String toString() {
    return "World{" + "name='" + name + '\'' + ", uuid=" + uuid + '}';
  }

  @Override
  public void dispose() {
    EventManager.INSTANCE.clear();
    worldTicker.dispose();
    synchronized (BOX2D_LOCK) {
      worldBody.dispose();
    }
    //    for (Entity entity : entities.values()) {
    //      entity.dispose();
    //    }
    //    entities.clear();
    //    players.clear();
    chunksLock.writeLock().lock();
    try {
      for (Chunk chunk : chunks.values()) {
        chunk.dispose();
      }
      chunks.clear();
    } finally {
      chunksLock.writeLock().unlock();
    }

    if (!transientWorld) {
      FileHandle worldFolder = getWorldFolder();
      if (worldFolder != null && worldFolder.isDirectory()) {
        if (!WorldLoader.deleteLockFile(uuid)) {
          Main.logger().error("Failed to delete world lock file!");
        }
      }
    }
  }
}
