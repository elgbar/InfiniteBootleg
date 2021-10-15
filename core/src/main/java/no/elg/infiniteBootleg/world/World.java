package no.elg.infiniteBootleg.world;

import static java.lang.Math.abs;
import static no.elg.infiniteBootleg.protobuf.ProtoWorld.World.Generator.EMPTY;
import static no.elg.infiniteBootleg.protobuf.ProtoWorld.World.Generator.FLAT;
import static no.elg.infiniteBootleg.protobuf.ProtoWorld.World.Generator.PERLIN;
import static no.elg.infiniteBootleg.protobuf.ProtoWorld.World.Generator.UNRECOGNIZED;
import static no.elg.infiniteBootleg.world.render.WorldRender.BOX2D_LOCK;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectSet;
import com.google.common.base.Preconditions;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.input.WorldInputHandler;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.util.Resizable;
import no.elg.infiniteBootleg.util.Ticker;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.util.ZipUtils;
import no.elg.infiniteBootleg.world.blocks.LightBlock;
import no.elg.infiniteBootleg.world.blocks.TickingBlock;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import no.elg.infiniteBootleg.world.generator.EmptyChunkGenerator;
import no.elg.infiniteBootleg.world.generator.FlatChunkGenerator;
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator;
import no.elg.infiniteBootleg.world.loader.ChunkLoader;
import no.elg.infiniteBootleg.world.loader.WorldLoader;
import no.elg.infiniteBootleg.world.render.HeadlessWorldRenderer;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import no.elg.infiniteBootleg.world.subgrid.MaterialEntity;
import no.elg.infiniteBootleg.world.subgrid.Removable;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import no.elg.infiniteBootleg.world.ticker.WorldTicker;
import no.elg.infiniteBootleg.world.time.WorldTime;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Different kind of views
 *
 * <ul>
 * <li>Chunk view: One unit in chunk view is {@link Chunk#CHUNK_SIZE} times larger than a unit in world view</li>
 * <li>World view: One unit in world view is {@link Block#BLOCK_SIZE} times larger than a unit in Box2D view</li>
 * <li>Box2D view: 1 (ie base unit)</li>
 * </ul>
 *
 * @author Elg
 */
public class World implements Disposable, Resizable {

    public static final short GROUND_CATEGORY = 0x1;
    public static final short LIGHTS_CATEGORY = 0x2;
    public static final short ENTITY_CATEGORY = 0x4;

    public static final Filter FALLING_BLOCK_ENTITY_FILTER;
    public static final Filter TRANSPARENT_BLOCK_ENTITY_FILTER;
    public static final Filter ENTITY_FILTER;
    public static final Filter LIGHT_FILTER;
    public static final Filter BLOCK_ENTITY_FILTER;

    public static final float SKYLIGHT_SOFTNESS_LENGTH = 3f;
    public static final float POINT_LIGHT_SOFTNESS_LENGTH = SKYLIGHT_SOFTNESS_LENGTH * 2f;

    static {
        //base filter for entities
        ENTITY_FILTER = new Filter();
        ENTITY_FILTER.categoryBits = ENTITY_CATEGORY;
        ENTITY_FILTER.maskBits = ENTITY_CATEGORY | GROUND_CATEGORY;

        //How light should collide
        LIGHT_FILTER = new Filter();
        LIGHT_FILTER.categoryBits = LIGHTS_CATEGORY;
        LIGHT_FILTER.maskBits = ENTITY_CATEGORY | GROUND_CATEGORY;

        //for falling blocks
        FALLING_BLOCK_ENTITY_FILTER = new Filter();
        FALLING_BLOCK_ENTITY_FILTER.categoryBits = ENTITY_CATEGORY;
        FALLING_BLOCK_ENTITY_FILTER.maskBits = GROUND_CATEGORY | LIGHTS_CATEGORY;

        //ie glass
        TRANSPARENT_BLOCK_ENTITY_FILTER = new Filter();
        TRANSPARENT_BLOCK_ENTITY_FILTER.categoryBits = GROUND_CATEGORY;
        TRANSPARENT_BLOCK_ENTITY_FILTER.maskBits = ENTITY_CATEGORY | GROUND_CATEGORY;

        //closed door
        BLOCK_ENTITY_FILTER = new Filter();
        BLOCK_ENTITY_FILTER.categoryBits = GROUND_CATEGORY;
        BLOCK_ENTITY_FILTER.maskBits = ENTITY_CATEGORY | GROUND_CATEGORY | LIGHTS_CATEGORY;
    }

    @NotNull
    private final UUID uuid;
    private final long seed;
    @NotNull
    private final WorldTicker worldTicker;
    @NotNull
    private final ChunkLoader chunkLoader;
    @NotNull
    private final WorldRender render;
    @NotNull
    private final WorldBody worldBody;
    @NotNull
    private final WorldTime worldTime;
    @NotNull
    private final Set<@NotNull Entity> entities = ConcurrentHashMap.newKeySet(); //all entities in this world (including living entities)
    @NotNull
    private final Set<@NotNull LivingEntity> livingEntities = ConcurrentHashMap.newKeySet(); //all player in this world
    @NotNull
    private final ConcurrentMap<@NotNull Location, @NotNull Chunk> chunks = new ConcurrentHashMap<>();
    @Nullable
    private volatile FileHandle worldFile;
    //only exists when graphics exits
    @Nullable
    private WorldInputHandler input;
    @NotNull
    private String name;

    private Location spawn;
    private final Lock loadLock = new ReentrantLock();

    public World(@NotNull ProtoWorld.World protoWorld) {
        this(WorldLoader.generatorFromProto(protoWorld), protoWorld.getSeed(), protoWorld.getName());
    }

    /**
     * Generate a world with a random seed
     */
    public World(@NotNull ChunkGenerator generator) {
        this(generator, MathUtils.random(Long.MAX_VALUE));
    }

    public World(@NotNull ChunkGenerator generator, long seed) {
        this(generator, seed, "World");
    }

    public World(@NotNull ChunkGenerator generator, long seed, @NotNull String worldName) {
        this.seed = seed;
        MathUtils.random.setSeed(seed);
        uuid = WorldLoader.getUUIDFromSeed(seed);

        name = worldName;

        worldTicker = new WorldTicker(this, false);

        chunkLoader = new ChunkLoader(this, generator);
        worldBody = new WorldBody(this);
        worldTime = new WorldTime(this);
        spawn = new Location(0, 0);

        if (Settings.client) {
            render = new WorldRender(this);
            input = new WorldInputHandler(render);
        }
        else {
            render = new HeadlessWorldRenderer(this);
        }
    }

    public void load() {
        FileHandle worldFolder = getWorldFolder();
        FileHandle worldZip = getWorldZip();

        if (!Settings.loadWorldFromDisk || worldFolder == null) {
            return;
        }
        if (worldZip == null || !worldZip.exists()) {
            Main.logger().log("No world save found");
            return;
        }
        Main.inst().getConsoleLogger().log("Loading world from '" + worldZip.file().getAbsolutePath() + '\'');

        worldFolder.deleteDirectory();
        ZipUtils.unzip(worldFolder, worldZip);

        var worldInfoFile = worldFolder.child(WorldLoader.WORLD_INFO_PATH);

        final ProtoWorld.World protoWorld;
        try {
            protoWorld = ProtoWorld.World.parseFrom(worldInfoFile.readBytes());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return;
        }

        spawn = Location.fromVector2i(protoWorld.getSpawn());
        worldTime.setTime(protoWorld.getTime());

        if (Settings.client && protoWorld.hasPlayer()) {
            final Player newPlayer = new Player(this, protoWorld.getPlayer());
            if (!newPlayer.isInvalid()) {
                addEntity(newPlayer, false);
                Main.inst().setPlayer(newPlayer);
            }
        }
        if (!worldTicker.isStarted()) {
            worldTicker.start();
        }
    }

    public void save() {
        if (!Settings.loadWorldFromDisk) {
            return;
        }
        FileHandle worldFolder = getWorldFolder();
        if (worldFolder == null) {
            return;
        }
        for (Chunk chunk : getChunks().values()) {
            chunkLoader.save(chunk);
        }

        var builder = ProtoWorld.World.newBuilder();
        builder.setName(name);
        builder.setSeed(seed);
        builder.setTime(worldTime.getTime());
        builder.setSpawn(spawn.toVector2i());
        builder.setGenerator(getGeneratorType());
        if (Settings.client) {
            final Player player = Main.inst().getPlayer();
            if (player != null) {
                builder.setPlayer(player.save());
            }
        }

        var worldInfoFile = worldFolder.child(WorldLoader.WORLD_INFO_PATH);
        if (worldInfoFile.exists()) {
            worldInfoFile.moveTo(worldFolder.child(WorldLoader.WORLD_INFO_PATH + ".old"));
        }
        worldInfoFile.writeBytes(builder.build().toByteArray(), false);

        FileHandle worldZip = worldFolder.parent().child(uuid + ".zip");
        try {
            ZipUtils.zip(worldFolder, worldZip);
            Main.logger().log("World", "World saved!");
        } catch (IOException e) {
            Main.logger().error("World", "Failed to save world due to a " + e.getClass().getSimpleName(), e);
        }
    }

    /**
     * @return The current folder of the world or {@code null} if no disk should be used
     */
    @Nullable
    public FileHandle getWorldFolder() {
        if (Settings.loadWorldFromDisk) {
            if (worldFile == null) {
                worldFile = WorldLoader.getWorldFolder(uuid);
            }
            return worldFile;
        }
        else {
            return null;
        }
    }

    protected FileHandle getWorldZip() {
        return WorldLoader.getWorldZip(getWorldFolder());
    }

    private ProtoWorld.World.Generator getGeneratorType() {
        final ChunkGenerator generator = chunkLoader.getGenerator();
        if (generator instanceof PerlinChunkGenerator) {
            return PERLIN;
        }
        else if (generator instanceof FlatChunkGenerator) {
            return FLAT;
        }
        else if (generator instanceof EmptyChunkGenerator) {
            return EMPTY;
        }
        else {
            return UNRECOGNIZED;
        }
    }

    @Nullable
    public Chunk getChunkFromWorld(int worldX, int worldY) {
        int chunkX = CoordUtil.worldToChunk(worldX);
        int chunkY = CoordUtil.worldToChunk(worldY);
        return getChunk(chunkX, chunkY);
    }

    @Nullable
    public Chunk getChunk(int chunkX, int chunkY) {
        return getChunk(new Location(chunkX, chunkY));
    }

    @Nullable
    public Chunk getChunk(@NotNull Location chunkLoc) {
        loadLock.lock();
        Chunk chunk = chunks.get(chunkLoc);
        try {
            if (chunk == null || !chunk.isLoaded()) {
                if (getWorldTicker().isPaused()) {
                    return null;
                }
                chunk = chunkLoader.load(chunkLoc.x, chunkLoc.y);
                Preconditions.checkState(chunk.isValid());
                chunks.put(chunkLoc, chunk);

            }
        } finally {
            loadLock.unlock();
        }
        return chunk;
    }

    /**
     * Set a block at a given location and update the textures
     *
     * @param worldLoc
     *     The location in world coordinates
     * @param material
     *     The new material to at given location
     *
     * @see Chunk#setBlock(int, int, Material, boolean)
     */
    public Chunk setBlock(@NotNull Location worldLoc, @Nullable Material material) {
        return setBlock(worldLoc, material, true);
    }

    /**
     * Set a block at a given location
     *
     * @param worldLoc
     *     The location in world coordinates
     * @param material
     *     The new material to at given location
     * @param update
     *     If the texture of the corresponding chunk should be updated
     *
     * @see Chunk#setBlock(int, int, Material, boolean)
     */
    public Chunk setBlock(@NotNull Location worldLoc, @Nullable Material material, boolean update) {
        return setBlock(worldLoc.x, worldLoc.y, material, update);
    }

    /**
     * Set a block at a given location
     *
     * @param worldX
     *     The x coordinate from world view
     * @param worldY
     *     The y coordinate from world view
     * @param material
     *     The new material to at given location
     * @param updateTexture
     *     If the texture of the corresponding chunk should be updated
     *
     * @see Chunk#setBlock(int, int, Material, boolean)
     */
    @Nullable
    public Chunk setBlock(int worldX, int worldY, @Nullable Material material, boolean updateTexture) {
        return setBlock(worldX, worldY, material, updateTexture, false);
    }

    public Chunk setBlock(int worldX, int worldY, @Nullable Material material, boolean updateTexture, boolean prioritize) {
        int chunkX = CoordUtil.worldToChunk(worldX);
        int chunkY = CoordUtil.worldToChunk(worldY);

        int localX = worldX - chunkX * Chunk.CHUNK_SIZE;
        int localY = worldY - chunkY * Chunk.CHUNK_SIZE;

        Chunk chunk = getChunk(chunkX, chunkY);
        if (chunk != null) {
            chunk.setBlock(localX, localY, material, updateTexture, prioritize);
        }
        return chunk;
    }

    /**
     * Set a block at a given location and update the textures
     *
     * @param worldX
     *     The x coordinate from world view
     * @param worldY
     *     The y coordinate from world view
     * @param material
     *     The new material to at given location
     *
     * @see Chunk#setBlock(int, int, Material, boolean)
     */
    public Chunk setBlock(int worldX, int worldY, @Nullable Material material) {
        return setBlock(worldX, worldY, material, true);
    }

    /**
     * Set a block at a given location
     *
     * @param worldX
     *     The x coordinate from world view
     * @param worldY
     *     The y coordinate from world view
     * @param block
     *     The block at the given location
     * @param update
     *     If the texture of the corresponding chunk should be updated
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

    /**
     * Remove anything that is at the given location be it a {@link Block} or {@link MaterialEntity}
     *
     * @param worldX
     *     The x coordinate from world view
     * @param worldY
     *     The y coordinate from world view
     * @param update
     *     If the texture of the corresponding chunk should be updated
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

    public Array<Entity> getEntities(float worldX, float worldY) {
        Array<Entity> foundEntities = new Array<>(false, 4);
        for (Entity entity : entities) {
            Vector2 pos = entity.getPosition();
            if (Util.isBetween(MathUtils.floor(pos.x - entity.getHalfBox2dWidth()), worldX, MathUtils.ceil(pos.x + entity.getHalfBox2dWidth())) && //
                Util.isBetween(MathUtils.floor(pos.y - entity.getHalfBox2dHeight()), worldY, MathUtils.ceil(pos.y + entity.getHalfBox2dHeight()))) {

                foundEntities.add(entity);
            }
        }
        return foundEntities;
    }

    /**
     * Check if a given location in the world is {@link Material#AIR} (or internally, doesn't exists) this is faster
     * than a
     * standard {@code getBlock(worldX, worldY).getMaterial == Material.AIR} as the {@link #getBlock(int, int, boolean)}
     * method might createBlock and store a new air block at the given location
     * <p>
     * <b>note</b> this does not if there are entities at this location
     *
     * @param worldLoc
     *     The world location to check
     *
     * @return If the block at the given location is air.
     */
    public boolean isAirBlock(@NotNull Location worldLoc) {
        return isAirBlock(worldLoc.x, worldLoc.y);
    }

    /**
     * Check if a given location in the world is {@link Material#AIR} (or internally, does not exist) this is faster
     * than a standard {@code getBlock(worldX, worldY).getMaterial == Material.AIR} as the {@link #getBlock(int, int,
     * boolean)} method might create a Block and store a new air block at the given location.
     * <p>
     * If the chunk at the given coordinates isn't loaded yet this method return `false` to prevent teleportation and
     * other actions that depend on an empty space.
     * <p>
     * <b>note</b> this does not if there are entities at this location
     *
     * @param worldX
     *     The x coordinate from world view
     * @param worldY
     *     The y coordinate from world view
     *
     * @return If the block at the given location is air.
     */
    public boolean isAirBlock(int worldX, int worldY) {
        int chunkX = CoordUtil.worldToChunk(worldX);
        int chunkY = CoordUtil.worldToChunk(worldY);

        int localX = worldX - chunkX * Chunk.CHUNK_SIZE;
        int localY = worldY - chunkY * Chunk.CHUNK_SIZE;

        Chunk chunk = getChunk(chunkX, chunkY);
        if (chunk == null) {
            //What should we return here? we don't really know as it does not exist.
            //Return false to prevent teleportation and other actions that depend on an empty space.
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
            //What should we return here? we don't really know as it does not exist.
            //Return false to prevent teleportation and other actions that depend on an empty space.
            return false;
        }

        Block b = chunk.getBlocks()[localX][localY];
        return b == null || !b.getMaterial().isSolid();
    }

    /**
     * Set all blocks in all cardinal directions around a given block to be updated. Given location not included
     *
     * @param worldX
     *     The x coordinate from world view
     * @param worldY
     *     The y coordinate from world view
     */
    public void updateBlocksAround(int worldX, int worldY) {
        for (Direction dir : Direction.CARDINAL) {
            Block rel = getBlock(worldX + dir.dx, worldY + dir.dy, true);
            if (rel instanceof TickingBlock tickingBlock) {
                tickingBlock.setShouldTick(true);
            }
        }
    }

    /**
     * Update all light sources currently loaded
     */
    public void updateLights() {
        for (Chunk chunk : getLoadedChunks()) {
            for (TickingBlock block : chunk.getTickingBlocks()) {
                if (block instanceof LightBlock) {
                    block.setShouldTick(true);
                }
            }
        }
    }

    /**
     * @param worldX
     *     The x coordinate from world view
     * @param worldY
     *     The y coordinate from world view
     * @param raw
     *
     * @return The block at the given x and y
     *
     * @see Chunk#getBlock(int, int)
     */
    @Nullable
    @Contract("_, _, false -> !null")
    public Block getBlock(int worldX, int worldY, boolean raw) {

        int chunkX = CoordUtil.worldToChunk(worldX);
        int chunkY = CoordUtil.worldToChunk(worldY);

        int localX = worldX - chunkX * Chunk.CHUNK_SIZE;
        int localY = worldY - chunkY * Chunk.CHUNK_SIZE;

        Chunk chunk = getChunk(chunkX, chunkY);
        if (chunk == null) {
            return null;
        }
        if (raw) {
            return chunk.getBlocks()[localX][localY];
        }
        else {
            return chunk.getBlock(localX, localY);
        }
    }

    /**
     * @return If the given chunk is loaded in memory
     */
    public boolean isChunkLoaded(int chunkX, int chunkY) {
        return isChunkLoaded(new Location(chunkX, chunkY));
    }

    /**
     * @param chunkLoc
     *     Chunk location in chunk coordinates
     *
     * @return If the given chunk is loaded in memory
     */
    public boolean isChunkLoaded(@NotNull Location chunkLoc) {
        Chunk chunk = chunks.get(chunkLoc);
        return chunk != null && chunk.isLoaded();
    }

    /**
     * Unload and save all chunks in this world.
     * <p>
     * Must be called on main thread!
     *
     * @param force
     *     If the chunks will be forced to unload
     */
    public void reload(boolean force) {
        var wasNotPaused = !worldTicker.isPaused();
        if (wasNotPaused) {
            worldTicker.pause();
        }
        Main.inst().getScheduler().waitForTasks();

        //remove all entities to speed up unloading
        for (Entity entity : getEntities()) {
            removeEntity(entity);
        }
        if (!entities.isEmpty() || !livingEntities.isEmpty()) {
            throw new IllegalStateException("Failed to clear entities during reload");
        }
        //ok to include unloaded chunks as they will not cause an error when unloading again
        for (Chunk chunk : chunks.values()) {
            unloadChunk(chunk, force, false);
        }
        if (!chunks.isEmpty()) {
            throw new IllegalStateException("Failed to clear chunks during reload");
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

        render.reload();

        load();

        if (wasNotPaused) {
            worldTicker.resume();
        }
        Main.logger().log("World", "World reloaded last save");
    }

    /**
     * @return All currently loaded chunks
     */
    public Collection<Chunk> getLoadedChunks() {
        return chunks.values().stream().filter(Chunk::isLoaded).collect(Collectors.toUnmodifiableSet());
    }


    /**
     * Unload the given chunks and save it to disk
     *
     * @param chunk
     *     The chunk to unload
     */
    public void unloadChunk(@Nullable Chunk chunk) {
        unloadChunk(chunk, false, true);
    }

    /**
     * Unload the given chunks and save it to disk
     *
     * @param chunk
     *     The chunk to unload
     * @param force
     *     If the chunk will be forced to unload
     * @param save
     *     If the chunk will be saved
     */
    public void unloadChunk(@Nullable Chunk chunk, boolean force, boolean save) {
        if (chunk != null && chunk.isLoaded() && (force || chunk.isAllowingUnloading())) {
            if (save) {
                chunkLoader.save(chunk);
            }
            for (Entity entity : chunk.getEntities()) {
                removeEntity(entity);
            }
            chunk.dispose();
            chunks.remove(new Location(chunk.getChunkX(), chunk.getChunkY()));
        }
    }

    /**
     * @param worldLoc
     *     The world location of this chunk
     *
     * @return The chunk at the given world location
     */
    @Nullable
    public Chunk getChunkFromWorld(@NotNull Location worldLoc) {
        return getChunk(CoordUtil.worldToChunk(worldLoc));
    }


    public boolean containsEntity(@NotNull UUID uuid) {
        return entities.stream().anyMatch(it -> uuid.equals(it.getUuid()));
    }

    /**
     * Add the given entity to entities in the world.
     * <b>NOTE</b> this is NOT automatically done when creating a new entity instance.
     *
     * @param entity
     *     The entity to add
     */
    public void addEntity(@NotNull Entity entity) { addEntity(entity, true); }

    /**
     * Add the given entity to entities in the world.
     * <b>NOTE</b> this is NOT automatically done when creating a new entity instance.
     *
     * @param entity
     *     The entity to add
     * @param loadChunk
     */
    public void addEntity(@NotNull Entity entity, boolean loadChunk) {
        if (entities.stream().anyMatch(it -> it == entity)) {
            Main.logger().error("World", "Tried to add entity twice to world " + entity.simpleName() + " " + entity.hudDebug());
            return;
        }
        if (containsEntity(entity.getUuid())) {
            Main.logger().error("World", "Tried to add duplicate entity to world " + entity.simpleName() + " " + entity.hudDebug());
            removeEntity(entity);
            return;
        }

        if (loadChunk) {
            //Load chunk of entity
            var chunk = getChunk(CoordUtil.worldToChunk(entity.getBlockX()), CoordUtil.worldToChunk(entity.getBlockY()));
            if (chunk == null) {
                //Failed to load chunk, remove entity
                Main.logger().error("World", "Failed to add entity to world, as its spawning chunk could not be loaded");
                removeEntity(entity);
                return;
            }
        }

        entities.add(entity);
        if (entity instanceof LivingEntity livingEntity) {
            livingEntities.add(livingEntity);
        }
    }

    /**
     * Remove and disposes the given entity.
     * <p>
     * Even if the given entity is not a part of this world, it will be disposed
     *
     * @param entity
     *     The entity to remove
     *
     * @throws IllegalArgumentException
     *     if the given entity is not part of this world
     */
    public void removeEntity(@NotNull Entity entity) {
        entities.remove(entity);
        if (entity instanceof LivingEntity) {
            livingEntities.remove(entity);
        }

        if (!entity.isInvalid()) {
            //even if we do not know of this entity, dispose it
            entity.dispose();
        }
    }

    /**
     * @param worldX
     *     X center (center of each block
     * @param worldY
     *     Y center
     * @param radius
     *     Radius to be equal or less from center
     * @param raw
     *     If blocks should be generated, if false this will return no null blocks
     *
     * @return Set of blocks within the given radius
     */
    @NotNull
    public ObjectSet<Block> getBlocksWithin(float worldX, float worldY, float radius, boolean raw) {
        Preconditions.checkArgument(radius >= 0, "Radius should be a non-negative number");
        ObjectSet<Block> blocks = new ObjectSet<>();
        float radiusSquare = radius * radius;
        for (Block block : getBlocksAABB(worldX, worldY, radius, radius, raw)) {
            if (abs(Vector2.dst2(worldX, worldY, block.getWorldX() + 0.5f, block.getWorldY() + 0.5f)) <= radiusSquare) {
                blocks.add(block);
            }
        }

        return blocks;
    }

    @NotNull
    public Array<Block> getBlocksAABB(float worldX, float worldY, float offsetX, float offsetY, boolean raw) {
        int capacity = MathUtils.floorPositive(abs(offsetX)) * MathUtils.floorPositive(abs(offsetY));
        Array<Block> blocks = new Array<>(true, capacity);
        int x = MathUtils.floor(worldX - offsetX);
        float maxX = worldX + offsetX;
        float maxY = worldY + offsetY;
        for (; x <= maxX; x++) {
            for (int y = MathUtils.floor(worldY - offsetY); y <= maxY; y++) {
                Block b = getBlock(x, y, raw);
                if (b == null) {
                    continue;
                }
                blocks.add(b);
            }
        }
        return blocks;
    }

    /**
     * @param worldX
     *     The x coordinate in world view
     * @param worldY
     *     The y coordinate in world view
     *
     * @return The first entity found within the given coordinates
     */
    @Nullable
    public Entity getEntity(float worldX, float worldY) {
        for (Entity entity : entities) {
            Vector2 pos = entity.getPosition();
            if (Util.isBetween(pos.x - entity.getHalfBox2dWidth(), worldX, pos.x + entity.getHalfBox2dWidth()) && //
                Util.isBetween(pos.y - entity.getHalfBox2dHeight(), worldY, pos.y + entity.getHalfBox2dHeight())) {
                return entity;
            }
        }
        return null;
    }

    /**
     * @param worldX
     *     The x coordinate in world view
     * @param worldY
     *     The y coordinate in world view
     *
     * @return The material at the given location
     */
    @NotNull
    public Material getMaterial(int worldX, int worldY) {
        Block block = getBlock(worldX, worldY, true);
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


    @Override
    public void resize(int width, int height) {
        if (Settings.client) {
            render.resize(width, height);
        }
    }

    @Nullable
    public WorldInputHandler getInput() {
        return input;
    }

    /**
     * @return Backing map of chunks
     */
    public @NotNull ConcurrentMap<Location, Chunk> getChunks() {
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

    public void setName(@NotNull String name) {
        this.name = name;
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
    public WorldRender getRender() {
        return render;
    }

    @NotNull
    public Ticker getWorldTicker() {
        return worldTicker;
    }

    /**
     * @return the current entities
     */
    public @NotNull Set<Entity> getEntities() {
        return entities;
    }

    public @NotNull Set<LivingEntity> getLivingEntities() {
        return livingEntities;
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
    public String toString() {
        return "World{" + "name='" + name + '\'' + ", uuid=" + uuid + '}';
    }

    @Override
    public void dispose() {
        getWorldTicker().stop();
        getInput().dispose();
        render.dispose();
        if (input != null) {
            input.dispose();
        }
    }
}
