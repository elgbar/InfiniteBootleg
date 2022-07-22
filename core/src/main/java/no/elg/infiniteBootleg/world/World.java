package no.elg.infiniteBootleg.world;

import static java.lang.Math.abs;
import static no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason.CHUNK_UNLOADED;
import static no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason.UNKNOWN_REASON;
import static no.elg.infiniteBootleg.protobuf.ProtoWorld.World.Generator.EMPTY;
import static no.elg.infiniteBootleg.protobuf.ProtoWorld.World.Generator.FLAT;
import static no.elg.infiniteBootleg.protobuf.ProtoWorld.World.Generator.PERLIN;
import static no.elg.infiniteBootleg.protobuf.ProtoWorld.World.Generator.UNRECOGNIZED;
import static no.elg.infiniteBootleg.world.GlobalLockKt.BOX2D_LOCK;
import static no.elg.infiniteBootleg.world.loader.WorldLoader.saveServerPlayer;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.LongArray;
import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.google.common.base.Preconditions;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.protobuf.Packets;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.server.PacketExtraKt;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.util.ExtraKt;
import no.elg.infiniteBootleg.util.Resizable;
import no.elg.infiniteBootleg.util.Ticker;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.util.ZipUtils;
import no.elg.infiniteBootleg.world.blocks.TickingBlock;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import no.elg.infiniteBootleg.world.generator.EmptyChunkGenerator;
import no.elg.infiniteBootleg.world.generator.FlatChunkGenerator;
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator;
import no.elg.infiniteBootleg.world.loader.ChunkLoader;
import no.elg.infiniteBootleg.world.loader.WorldLoader;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.MaterialEntity;
import no.elg.infiniteBootleg.world.subgrid.Removable;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
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

  public static final short GROUND_CATEGORY = 0b0000_0000_0000_0001;
  public static final short LIGHTS_CATEGORY = 0b0000_0000_0000_0010;
  public static final short ENTITY_CATEGORY = 0b0000_0000_0000_0100;

  public static final Filter FALLING_BLOCK_ENTITY_FILTER;
  public static final Filter FALLING_BLOCK_BLOCKS_LIGHT_ENTITY_FILTER;
  public static final Filter TRANSPARENT_BLOCK_ENTITY_FILTER;
  public static final Filter ENTITY_FILTER;
  public static final Filter LIGHT_FILTER;
  public static final Filter BLOCK_ENTITY_FILTER;
  public static final Filter NON_INTERACTIVE_FILTER;
  public static final Filter NON_INTERACTIVE_GROUND_FILTER;

  public static final float SKYLIGHT_SOFTNESS_LENGTH = 3f;
  public static final float POINT_LIGHT_SOFTNESS_LENGTH = SKYLIGHT_SOFTNESS_LENGTH * 2f;

  public static final String LOCK_FILE_NAME = ".locked";

  static {
    // base filter for entities
    ENTITY_FILTER = new Filter();
    ENTITY_FILTER.categoryBits = ENTITY_CATEGORY;
    ENTITY_FILTER.maskBits = ENTITY_CATEGORY | GROUND_CATEGORY;

    // How light should collide
    LIGHT_FILTER = new Filter();
    LIGHT_FILTER.categoryBits = LIGHTS_CATEGORY;
    LIGHT_FILTER.maskBits = ENTITY_CATEGORY | GROUND_CATEGORY;

    // for falling blocks
    FALLING_BLOCK_ENTITY_FILTER = new Filter();
    FALLING_BLOCK_ENTITY_FILTER.categoryBits = ENTITY_CATEGORY;
    FALLING_BLOCK_ENTITY_FILTER.maskBits = GROUND_CATEGORY;

    // for falling blocks which blocks light
    FALLING_BLOCK_BLOCKS_LIGHT_ENTITY_FILTER = new Filter();
    FALLING_BLOCK_BLOCKS_LIGHT_ENTITY_FILTER.categoryBits = ENTITY_CATEGORY;
    FALLING_BLOCK_BLOCKS_LIGHT_ENTITY_FILTER.maskBits = GROUND_CATEGORY | LIGHTS_CATEGORY;

    // ie glass
    TRANSPARENT_BLOCK_ENTITY_FILTER = new Filter();
    TRANSPARENT_BLOCK_ENTITY_FILTER.categoryBits = GROUND_CATEGORY;
    TRANSPARENT_BLOCK_ENTITY_FILTER.maskBits = ENTITY_CATEGORY | GROUND_CATEGORY;

    // closed door
    BLOCK_ENTITY_FILTER = new Filter();
    BLOCK_ENTITY_FILTER.categoryBits = GROUND_CATEGORY;
    BLOCK_ENTITY_FILTER.maskBits = ENTITY_CATEGORY | GROUND_CATEGORY | LIGHTS_CATEGORY;

    // Frozen body, should not interact with anything
    NON_INTERACTIVE_FILTER = new Filter();
    NON_INTERACTIVE_FILTER.categoryBits = 0;
    NON_INTERACTIVE_FILTER.maskBits = 0;

    // Frozen body, should not interact with anything
    NON_INTERACTIVE_GROUND_FILTER = new Filter();
    NON_INTERACTIVE_GROUND_FILTER.categoryBits = GROUND_CATEGORY;
    NON_INTERACTIVE_GROUND_FILTER.maskBits = 0;
  }

  @NotNull private final UUID uuid;
  private final long seed;
  @NotNull private final WorldTicker worldTicker;
  @NotNull private final ChunkLoader chunkLoader;
  @NotNull private final WorldBody worldBody;
  @NotNull private final WorldTime worldTime;

  @NotNull
  private final Map<@NotNull UUID, @NotNull Entity> entities =
      new ConcurrentHashMap<>(); // all entities in this world (including living entities)

  @NotNull
  private final Map<@NotNull UUID, @NotNull Player> players =
      new ConcurrentHashMap<>(); // all player in this world

  /** must be accessed under {@link #chunkLock} */
  @NotNull protected final LongMap<Chunk> chunks = new LongMap<>();

  @Nullable private volatile FileHandle worldFile;

  @NotNull private final String name;

  private Location spawn;
  private final ReentrantReadWriteLock chunkLock = new ReentrantReadWriteLock();
  public final Lock chunksReadLock = chunkLock.readLock();
  public final Lock chunksWriteLock = chunkLock.writeLock();

  private boolean transientWorld = !Settings.loadWorldFromDisk;

  public World(@NotNull ProtoWorld.World protoWorld) {
    this(WorldLoader.generatorFromProto(protoWorld), protoWorld.getSeed(), protoWorld.getName());
  }

  public World(@NotNull ChunkGenerator generator, long seed, @NotNull String worldName) {
    this.seed = seed;
    MathUtils.random.setSeed(seed);
    uuid = ExtraKt.generateUUIDFromName("" + seed);

    name = worldName;

    worldTicker = new WorldTicker(this, false);

    chunkLoader = new ChunkLoader(this, generator);
    worldBody = new WorldBody(this);
    worldTime = new WorldTime(this);
    spawn = new Location(0, 0);
  }

  public void initialize() {
    if (Settings.loadWorldFromDisk) {
      FileHandle worldFolder = getWorldFolder();
      if (worldFolder != null
          && worldFolder.exists()
          && worldFolder.child(LOCK_FILE_NAME).exists()) {
        transientWorld = true;
        Main.logger()
            .warn(
                "World", "World found is already in use. Initializing this as a transient world.");
      }

      FileHandle worldZip = getWorldZip();
      if (transientWorld || worldFolder == null || worldZip == null || !worldZip.exists()) {
        Main.logger().log("No world save found");
      } else {
        Main.logger().log("Loading world from '" + worldZip.file().getAbsolutePath() + '\'');

        worldFolder.deleteDirectory();
        var lockFile = worldFolder.child(LOCK_FILE_NAME);
        lockFile.writeString(ProcessHandle.current().pid() + "\n", true);
        ZipUtils.unzip(worldFolder, worldZip);

        var worldInfoFile = worldFolder.child(WorldLoader.WORLD_INFO_PATH);

        final ProtoWorld.World protoWorld;
        try {
          protoWorld = ProtoWorld.World.parseFrom(worldInfoFile.readBytes());
          loadFromProtoWorld(protoWorld);
        } catch (InvalidProtocolBufferException e) {
          e.printStackTrace();
        }
      }
    }
    if (Main.isSingleplayer() && ClientMain.inst().getPlayer() == null) {
      ClientMain.inst().setPlayer(new Player(this, spawn.x, spawn.y));
    }
    getRender().update();
    if (!worldTicker.isStarted()) {
      worldTicker.start();
    }
  }

  @NotNull
  public Player createNewPlayer(@NotNull UUID playerId) {
    Player player = new Player(this, spawn.x, spawn.y, playerId);
    Preconditions.checkState(!player.isDisposed());
    addEntity(player);
    return player;
  }

  public void loadFromProtoWorld(@NotNull ProtoWorld.World protoWorld) {
    spawn = Location.fromVector2i(protoWorld.getSpawn());
    worldTime.setTimeScale(protoWorld.getTimeScale());
    worldTime.setTime(protoWorld.getTime());

    if (Main.isSingleplayer() && protoWorld.hasPlayer()) {
      ProtoWorld.Entity protoWorldPlayer = protoWorld.getPlayer();
      if (ExtraKt.fromUUIDOrNull(protoWorldPlayer.getUuid()) != null) {
        ClientMain.inst().setPlayer(new Player(this, protoWorldPlayer));
      } else {
        Main.logger()
            .error("World", "Failed to load player from world. The world might be corrupt.");
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

    for (Chunk chunk : getLoadedChunks()) {
      chunkLoader.save(chunk);
    }

    for (Player player : players.values()) {
      saveServerPlayer(player);
    }

    var builder = toProtobuf();

    var worldInfoFile = worldFolder.child(WorldLoader.WORLD_INFO_PATH);
    if (worldInfoFile.exists()) {
      worldInfoFile.moveTo(worldFolder.child(WorldLoader.WORLD_INFO_PATH + ".old"));
    }
    worldInfoFile.writeBytes(builder.toByteArray(), false);

    FileHandle worldZip = worldFolder.parent().child(uuid + ".zip");
    try {
      ZipUtils.zip(worldFolder, worldZip);
      Main.logger().log("World", "World saved!");
    } catch (IOException e) {
      Main.logger()
          .error("World", "Failed to save world due to a " + e.getClass().getSimpleName(), e);
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
    if (Main.isSingleplayer()) {
      final Player player = ClientMain.inst().getPlayer();
      if (player != null) {
        builder.setPlayer(player.save());
      }
    }
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

  protected FileHandle getWorldZip() {
    return WorldLoader.getWorldZip(getWorldFolder());
  }

  private ProtoWorld.World.Generator getGeneratorType() {
    final ChunkGenerator generator = chunkLoader.getGenerator();
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

  @Nullable
  public Chunk getChunkFromWorld(int worldX, int worldY) {
    int chunkX = CoordUtil.worldToChunk(worldX);
    int chunkY = CoordUtil.worldToChunk(worldY);
    return getChunk(chunkX, chunkY);
  }

  public void updateChunk(@NotNull Chunk chunk) {
    Preconditions.checkState(chunk.isValid());
    chunksWriteLock.lock();

    Chunk old;
    try {
      old = chunks.put(chunk.getCompactLocation(), chunk);
    } finally {
      chunksWriteLock.unlock();
    }
    if (old != null) {
      old.dispose();
    }
  }

  @Nullable
  public Chunk getChunk(@NotNull Location chunkLoc) {
    return getChunk(chunkLoc.toCompactLocation());
  }

  @Nullable
  public Chunk getChunk(int chunkX, int chunkY) {
    return getChunk(CoordUtil.compactLoc(chunkX, chunkY));
  }

  @Nullable
  public Chunk getChunk(long chunkLoc) {
    // This is a long lock, it must appear to be an atomic operation though
    Chunk readChunk;
    Chunk old = null;
    boolean acquiredLock;
    try {
      acquiredLock = chunksReadLock.tryLock(100, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      acquiredLock = false;
    }
    if (acquiredLock) {
      try {
        readChunk = chunks.get(chunkLoc);
      } finally {
        chunksReadLock.unlock();
      }
    } else {
      Main.logger().warn("World", "Failed to acquire chunks read lock in 100 ms");
      return null;
    }
    if (readChunk == null || !readChunk.isLoaded()) {
      if (getWorldTicker().isPaused()) {
        Main.logger().debug("World", "Ticker paused will not load chunk");
        return null;
      }
      chunksWriteLock.lock();
      try {
        // another thread might have loaded the chunk while we were waiting here
        if (readChunk != null && readChunk.isLoaded()) {
          return readChunk;
        }
        if (readChunk != null) {
          readChunk.dispose();
        }
        Chunk loadedChunk = chunkLoader.load(chunkLoc);
        if (loadedChunk == null) {
          old = chunks.remove(chunkLoc);
          //          Main.logger().warn("World", "removed chunk " +
          // CoordUtil.stringifyCompactLoc(chunkLoc) + ": failed to load chunk. Chunk size is now "
          // + chunks.size);
          return null;
        } else {
          Preconditions.checkState(loadedChunk.isValid());
          old = chunks.put(chunkLoc, loadedChunk);
          return loadedChunk;
        }
      } finally {
        chunksWriteLock.unlock();
        if (old != null) {
          old.dispose();
        }
      }
    }
    return readChunk;
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
    return setBlock(worldLoc.x, worldLoc.y, material, update);
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

    Chunk chunk = getChunk(chunkX, chunkY);
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

    Chunk chunk = getChunk(chunkX, chunkY);
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

    Chunk chunk = getChunk(chunkX, chunkY);
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

    for (Entity entity : getEntities(worldX, worldY)) {
      if (entity instanceof Removable) {
        removeEntity(entity);
      }
    }

    Chunk chunk = getChunk(chunkX, chunkY);
    if (chunk != null) {
      chunk.setBlock(localX, localY, (Block) null, update);
    }
  }

  public void removeBlocks(ObjectSet<? extends @NotNull Block> blocks, boolean prioritize) {
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
    for (Entity entity : entities.values()) {
      Vector2 pos = entity.getPosition();
      if (Util.isBetween(
              MathUtils.floor(pos.x - entity.getHalfBox2dWidth()),
              worldX,
              MathUtils.ceil(pos.x + entity.getHalfBox2dWidth()))
          && //
          Util.isBetween(
              MathUtils.floor(pos.y - entity.getHalfBox2dHeight()),
              worldY,
              MathUtils.ceil(pos.y + entity.getHalfBox2dHeight()))) {

        foundEntities.add(entity);
      }
    }
    return foundEntities;
  }

  /**
   * Check if a given location in the world is {@link Material#AIR} (or internally, doesn't exists)
   * this is faster than a standard {@code getBlock(worldX, worldY).getMaterial == Material.AIR} as
   * the {@link #getRawBlock(int, int)} method might createBlock and store a new air block at the
   * given location
   *
   * <p><b>note</b> this does not if there are entities at this location
   *
   * @param worldLoc The world location to check
   * @return If the block at the given location is air.
   */
  public boolean isAirBlock(@NotNull Location worldLoc) {
    return isAirBlock(worldLoc.x, worldLoc.y);
  }

  public boolean isAirBlock(long compactWorldLoc) {
    return isAirBlock(
        CoordUtil.decompactLocX(compactWorldLoc), CoordUtil.decompactLocY(compactWorldLoc));
  }

  /**
   * Check if a given location in the world is {@link Material#AIR} (or internally, does not exist)
   * this is faster than a standard {@code getBlock(worldX, worldY).getMaterial == Material.AIR} as
   * the {@link #getRawBlock(int, int)} method might create a Block and store a new air block at the
   * given location.
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

    Chunk chunk = getChunk(chunkX, chunkY);
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

    Chunk chunk = getChunk(chunkX, chunkY);
    if (chunk == null) {
      // What should we return here? we don't really know as it does not exist.
      // Return false to prevent teleportation and other actions that depend on an empty space.
      return false;
    }

    Block b = chunk.getBlocks()[localX][localY];
    return b == null || !b.getMaterial().isSolid();
  }

  /**
   * Set all blocks in all cardinal directions around a given block to be updated. Given location
   * not included
   *
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   */
  public void updateBlocksAround(int worldX, int worldY) {
    for (Direction dir : Direction.CARDINAL) {
      Block rel = getRawBlock(worldX + dir.dx, worldY + dir.dy);
      if (rel instanceof TickingBlock tickingBlock) {
        tickingBlock.enableTick();
      }
    }
  }

  /**
   * @param compactWorldLoc The coordinates from world view in a compact form
   * @return The block at the given x and y
   */
  @Nullable
  public Block getRawBlock(long compactWorldLoc) {
    return getRawBlock(
        CoordUtil.decompactLocX(compactWorldLoc), CoordUtil.decompactLocY(compactWorldLoc));
  }

  /**
   * @param worldX The x coordinate from world view
   * @param worldY The y coordinate from world view
   * @return The block at the given x and y (or null if air block)
   */
  @Nullable
  public Block getRawBlock(int worldX, int worldY) {
    int chunkX = CoordUtil.worldToChunk(worldX);
    int chunkY = CoordUtil.worldToChunk(worldY);

    int localX = worldX - chunkX * Chunk.CHUNK_SIZE;
    int localY = worldY - chunkY * Chunk.CHUNK_SIZE;

    Chunk chunk = getChunk(chunkX, chunkY);
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
    int chunkX = CoordUtil.worldToChunk(worldX);
    int chunkY = CoordUtil.worldToChunk(worldY);

    int localX = worldX - chunkX * Chunk.CHUNK_SIZE;
    int localY = worldY - chunkY * Chunk.CHUNK_SIZE;

    Chunk chunk = getChunk(chunkX, chunkY);
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
    chunksReadLock.lock();
    try {
      chunk = chunks.get(compactedChunkLoc);
    } finally {
      chunksReadLock.unlock();
    }
    return chunk != null && chunk.isLoaded();
  }

  /**
   * Unload and save all chunks in this world.
   *
   * <p>Must be called on main thread!
   *
   * @param force If the chunks will be forced to unload
   */
  public void reload(boolean force) {
    if (true) {
      return;
    }
    var wasNotPaused = !worldTicker.isPaused();
    if (wasNotPaused) {
      worldTicker.pause();
    }
    Main.inst().getScheduler().waitForTasks();

    // remove all entities to speed up unloading
    for (Entity entity : getEntities()) {
      removeEntity(entity);
    }
    if (!entities.isEmpty() || !players.isEmpty()) {
      throw new IllegalStateException("Failed to clear entities during reload");
    }
    // ok to include unloaded chunks as they will not cause an error when unloading again
    chunksWriteLock.lock();
    try {
      for (Chunk chunk : chunks.values()) {
        unloadChunk(chunk, force, false);
      }

      if (!chunks.isEmpty()) {
        throw new IllegalStateException("Failed to clear chunks during reload");
      }
    } finally {
      chunksWriteLock.unlock();
    }
    synchronized (BOX2D_LOCK) {
      var bodies = new Array<@NotNull Body>(false, worldBody.getBox2dWorld().getBodyCount());
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
  }

  /**
   * @return All currently loaded chunks
   */
  @NotNull
  public Array<@NotNull Chunk> getLoadedChunks() {
    chunksReadLock.lock();
    try {
      var loadedChunks = new Array<@NotNull Chunk>(true, chunks.size, Chunk.class);
      for (Chunk chunk : chunks.values()) {
        if (chunk != null && chunk.isLoaded()) {
          loadedChunks.add(chunk);
        }
      }
      return loadedChunks;
    } finally {
      chunksReadLock.unlock();
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
   */
  public void unloadChunk(@Nullable Chunk chunk, boolean force, boolean save) {
    if (chunk != null && chunk.isLoaded() && (force || chunk.isAllowingUnloading())) {
      if (chunk.getWorld() != this) {
        Main.logger().warn("Tried to unload chunk from different world");
        return;
      }
      Chunk removedChunk;
      chunksWriteLock.lock();
      try {
        if (save) {
          chunkLoader.save(chunk);
        }
        for (Entity entity : chunk.getEntities()) {
          removeEntity(entity, CHUNK_UNLOADED);
        }
        long compactLocation = chunk.getCompactLocation();
        removedChunk = chunks.remove(compactLocation);
        //        Main.logger().warn("World", "removed chunk " +
        // CoordUtil.stringifyCompactLoc(compactLocation) + ": unload chunk. Chunk size is now " +
        // chunks.size);
      } finally {
        chunksWriteLock.unlock();
      }

      chunk.dispose();
      if (removedChunk != null && chunk != removedChunk) {
        Main.logger().warn("Removed unloaded chunk was different from chunk in chunks");
        removedChunk.dispose();
      }
    }
  }

  /**
   * @param worldLoc The world location of this chunk
   * @return The chunk at the given world location
   */
  @Nullable
  public Chunk getChunkFromWorld(@NotNull Location worldLoc) {
    return getChunk(CoordUtil.worldToChunk(worldLoc));
  }

  public boolean containsEntity(@NotNull UUID uuid) {
    return entities.containsKey(uuid);
  }

  @Nullable
  public Entity getEntity(@NotNull UUID uuid) {
    return entities.get(uuid);
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
   * @param loadChunk
   */
  public void addEntity(@NotNull Entity entity, boolean loadChunk) {
    if (entities.containsValue(entity)) {
      Main.logger()
          .error(
              "World",
              "Tried to add entity twice to world "
                  + entity.simpleName()
                  + " "
                  + entity.hudDebug());
      return;
    }
    if (containsEntity(entity.getUuid())) {
      Main.logger()
          .error(
              "World",
              "Tried to add duplicate entity to world "
                  + entity.simpleName()
                  + " "
                  + entity.hudDebug());
      removeEntity(entity);
      return;
    }

    if (loadChunk) {
      // Load chunk of entity
      var chunk =
          getChunk(
              CoordUtil.worldToChunk(entity.getBlockX()),
              CoordUtil.worldToChunk(entity.getBlockY()));
      if (chunk == null) {
        // Failed to load chunk, remove entity
        Main.logger()
            .error(
                "World",
                "Failed to add entity to world, as its spawning chunk could not be loaded");
        removeEntity(entity);
        return;
      }
    }

    entities.put(entity.getUuid(), entity);
    if (entity instanceof Player player) {
      players.put(player.getUuid(), player);
      saveServerPlayer(player);
    }
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
    final UUID entityUuid = entity.getUuid();
    entities.remove(entityUuid);
    if (entity instanceof Player player) {
      players.remove(entityUuid);
      saveServerPlayer(player);
    }
    if (!entity.isDisposed()) {
      // even if we do not know of this entity, dispose it
      entity.dispose();
    }
    if (Main.isServer()) {
      Main.inst()
          .getScheduler()
          .executeSync(
              () ->
                  PacketExtraKt.broadcast(
                      PacketExtraKt.clientBoundDespawnEntity(entityUuid, reason), null));
    }
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
      if (abs(Vector2.dst2(worldX, worldY, blockWorldX + 0.5f, blockWorldY + 0.5f))
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

  @NotNull
  public Array<@NotNull Block> getBlocksAABB(
      float worldX, float worldY, float offsetX, float offsetY) {
    int capacity = MathUtils.floorPositive(abs(offsetX)) * MathUtils.floorPositive(abs(offsetY));
    Array<Block> blocks = new Array<>(true, capacity);
    int x = MathUtils.floor(worldX - offsetX);
    float maxX = worldX + offsetX;
    float maxY = worldY + offsetY;
    for (; x <= maxX; x++) {
      for (int y = MathUtils.floor(worldY - offsetY); y <= maxY; y++) {
        Block b = getRawBlock(x, y);
        if (b == null) {
          continue;
        }
        blocks.add(b);
      }
    }
    return blocks;
  }

  @NotNull
  public LongArray getLocationsAABB(float worldX, float worldY, float offsetX, float offsetY) {
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
    for (Entity entity : entities.values()) {
      Vector2 pos = entity.getPosition();
      if (Util.isBetween(
              pos.x - entity.getHalfBox2dWidth(), worldX, pos.x + entity.getHalfBox2dWidth())
          && //
          Util.isBetween(
              pos.y - entity.getHalfBox2dHeight(), worldY, pos.y + entity.getHalfBox2dHeight())) {
        return entity;
      }
    }
    return null;
  }

  /**
   * @param worldX The x coordinate in world view
   * @param worldY The y coordinate in world view
   * @return The material at the given location
   */
  @NotNull
  public Material getMaterial(int worldX, int worldY) {
    Block block = getRawBlock(worldX, worldY);
    if (block != null) {
      return block.getMaterial();
    }

    for (Entity entity : getEntities(worldX, worldY)) {
      if (entity instanceof MaterialEntity materialEntity) {
        return materialEntity.getMaterial();
      }
    }
    return Material.AIR;
  }

  /** Alias to {@code WorldBody#postBox2dRunnable} */
  public void postBox2dRunnable(Runnable runnable) {
    worldBody.postBox2dRunnable(runnable);
  }

  /**
   * Must be accessed under {@link #chunksReadLock} or {@link #chunksWriteLock}
   *
   * @return Backing map of chunks
   */
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
  public @NotNull UUID getUuid() {
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

  /**
   * @return the current entities
   */
  public @NotNull Collection<Entity> getEntities() {
    return new HashSet<>(entities.values());
  }

  public @NotNull Collection<Player> getPlayers() {
    return new HashSet<>(players.values());
  }

  public boolean hasPlayer(@NotNull UUID uuid) {
    return players.containsKey(uuid);
  }

  @Nullable
  public Player getPlayer(@Nullable UUID uuid) {
    return players.get(uuid);
  }

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
    worldTicker.dispose();
    worldBody.dispose();
    for (Entity entity : entities.values()) {
      entity.dispose();
    }
    entities.clear();
    players.clear();
    chunksWriteLock.lock();
    try {
      for (Chunk chunk : chunks.values()) {
        chunk.dispose();
      }
      chunks.clear();
    } finally {
      chunksWriteLock.unlock();
    }

    if (!transientWorld) {
      FileHandle worldFolder = getWorldFolder();
      if (worldFolder != null) {
        if (!worldFolder.deleteDirectory()) {
          Main.logger()
              .warn(
                  "World",
                  "Failed to delete world directory, trying to explicitly delete lock file");
          FileHandle child = worldFolder.child(LOCK_FILE_NAME);
          Path path = child.file().toPath();
          try {
            Files.delete(path);
            Main.logger()
                .log("World", "Deleted world lock file after failing to delete world folder");
          } catch (IOException e) {
            Main.logger().warn("World", "Failed to delete lock file: " + e.getMessage());
          }
        } else {
          Main.logger().log("World", "Deleted world directory");
        }
      }
    }
  }
}
