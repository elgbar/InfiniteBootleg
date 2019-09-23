package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.input.WorldInputHandler;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.util.Resizable;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.util.ZipUtils;
import no.elg.infiniteBootleg.world.blocks.UpdatableBlock;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import no.elg.infiniteBootleg.world.loader.ChunkLoader;
import no.elg.infiniteBootleg.world.render.HeadlessWorldRenderer;
import no.elg.infiniteBootleg.world.render.Updatable;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.MaterialEntity;
import no.elg.infiniteBootleg.world.subgrid.Removable;
import no.elg.infiniteBootleg.world.subgrid.box2d.ContactManager;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
public class World implements Disposable, Updatable, Resizable {

    public static final short GROUND_CATEGORY = 0x1;
    public static final short LIGHTS_CATEGORY = 0x2;
    public static final short ENTITY_CATEGORY = 0x4;

    public static final Filter BLOCK_ENTITY_FILTER;
    public static final Filter SOLID_TRANSPARENT_FILTER;
    public static final Filter ENTITY_FILTER;
    public static final Filter LIGHT_FILTER;

    public static final int MAX_DEG_SKYLIGHT = -45;
    public static final int MIN_DEG_SKYLIGHT = -135;
    public static final int STRAIGHT_DOWN_SKYLIGHT = -90;

    static {
        //base filter for entities
        ENTITY_FILTER = new Filter();
        ENTITY_FILTER.categoryBits = ENTITY_CATEGORY;
        ENTITY_FILTER.maskBits = ENTITY_CATEGORY | GROUND_CATEGORY;

        //skylight
        LIGHT_FILTER = new Filter();
        LIGHT_FILTER.categoryBits = LIGHTS_CATEGORY;
        LIGHT_FILTER.maskBits = ENTITY_CATEGORY | GROUND_CATEGORY;

        //for falling blocks
        BLOCK_ENTITY_FILTER = new Filter();
        BLOCK_ENTITY_FILTER.categoryBits = GROUND_CATEGORY;
        BLOCK_ENTITY_FILTER.maskBits = ENTITY_CATEGORY | GROUND_CATEGORY | LIGHTS_CATEGORY;

        //ie glass
        SOLID_TRANSPARENT_FILTER = new Filter();
        SOLID_TRANSPARENT_FILTER.categoryBits = GROUND_CATEGORY;
        SOLID_TRANSPARENT_FILTER.maskBits = ENTITY_CATEGORY | GROUND_CATEGORY;
    }

    public static boolean dayTicking = true;

    private final UUID uuid;
    private final long seed;
    private final Map<Location, Chunk> chunks;
    private final WorldTicker ticker;
    private final ChunkLoader chunkLoader;
    private final com.badlogic.gdx.physics.box2d.World box2dWorld;
    private FileHandle worldFile;

    //only exists when graphics exits
    private WorldInputHandler input;
    private WorldRender render;

    private Set<Entity> entities; //all entities in this world (inc players)
    private Set<Player> players; //all player in this world

    private String name = "World";
    private int time;


    /**
     * Generate a world with a random seed
     *
     * @param generator
     */
    public World(@NotNull ChunkGenerator generator) {
        this(generator, MathUtils.random(Long.MAX_VALUE));
    }

    public World(@NotNull ChunkGenerator generator, long seed) {
        this.seed = seed;
        MathUtils.random.setSeed(seed);
        chunks = new ConcurrentHashMap<>();
        entities = ConcurrentHashMap.newKeySet();
        players = ConcurrentHashMap.newKeySet();
        time = STRAIGHT_DOWN_SKYLIGHT;

        byte[] UUIDSeed = new byte[128];
        MathUtils.random.nextBytes(UUIDSeed);
        uuid = UUID.nameUUIDFromBytes(UUIDSeed);

        chunkLoader = new ChunkLoader(this, generator);
        ticker = new WorldTicker(this);


        box2dWorld = new com.badlogic.gdx.physics.box2d.World(new Vector2(0f, -10), true);
        box2dWorld.setContactListener(new ContactManager(this));


        if (Main.renderGraphic) {
            render = new WorldRender(this);
            input = new WorldInputHandler(render);

            Gdx.app.postRunnable(() -> new Player(this));
        }
        else {
            render = new HeadlessWorldRenderer(this);
        }
        load();
    }

    @NotNull
    public Chunk getChunk(int chunkX, int chunkY) {
        return getChunk(new Location(chunkX, chunkY));
    }

    @NotNull
    public Chunk getChunk(@NotNull Location chunkLoc) {
        Chunk chunk = chunks.get(chunkLoc);
        if (chunk == null) {
            chunk = chunkLoader.load(chunkLoc.x, chunkLoc.y);
            chunks.put(chunkLoc, chunk);
        }
        return chunk;
    }

    @NotNull
    public Chunk getChunkFromWorld(int worldX, int worldY) {
        int chunkX = CoordUtil.worldToChunk(worldX);
        int chunkY = CoordUtil.worldToChunk(worldY);
        return getChunk(chunkX, chunkY);
    }

    /**
     * @param worldX
     *     The x coordinate from world view
     * @param worldY
     *     The y coordinate from world view
     *
     * @return The block at the given x and y
     *
     * @see Chunk#getBlock(int, int)
     */
    @NotNull
    public Block getBlock(int worldX, int worldY) {

        int chunkX = CoordUtil.worldToChunk(worldX);
        int chunkY = CoordUtil.worldToChunk(worldY);

        int localX = worldX - chunkX * Chunk.CHUNK_SIZE;
        int localY = worldY - chunkY * Chunk.CHUNK_SIZE;

        return getChunk(chunkX, chunkY).getBlock(localX, localY);
    }

    /**
     * @param worldX
     *     The x coordinate from world view
     * @param worldY
     *     The y coordinate from world view
     *
     * @return The block at the given location, Air can either be null or a block with material {@link Material#AIR}
     */
    @Nullable
    public Block getRawBlock(int worldX, int worldY) {
        int chunkX = CoordUtil.worldToChunk(worldX);
        int chunkY = CoordUtil.worldToChunk(worldY);

        int localX = CoordUtil.chunkOffset(worldX);
        int localY = CoordUtil.chunkOffset(worldY);

        return getChunk(chunkX, chunkY).getBlocks()[localX][localY];
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
     * @param material
     *     The new material to at given location
     * @param update
     *     If the texture of the corresponding chunk should be updated
     *
     * @see Chunk#setBlock(int, int, Material, boolean)
     */
    public Chunk setBlock(int worldX, int worldY, @Nullable Material material, boolean update) {
        int chunkX = CoordUtil.worldToChunk(worldX);
        int chunkY = CoordUtil.worldToChunk(worldY);

        int localX = worldX - chunkX * Chunk.CHUNK_SIZE;
        int localY = worldY - chunkY * Chunk.CHUNK_SIZE;

        Chunk chunk = getChunk(chunkX, chunkY);
        chunk.setBlock(localX, localY, material, update);
        return chunk;
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
        chunk.setBlock(localX, localY, block, update);
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

        Chunk chunk = getChunk(chunkX, chunkY);
        chunk.setBlock(localX, localY, (Block) null, update);
        //noinspection LibGDXUnsafeIterator
        for (Entity entity : getEntities(worldX, worldY)) {
            if (entity instanceof Removable) {
                ((Removable) entity).onRemove();
                removeEntity(entity);
            }
        }
    }

    /**
     * Check if a given location in the world is {@link Material#AIR} (or internally, doesn't exists) this is faster
     * than a
     * standard {@code getBlock(worldX, worldY).getMaterial == Material.AIR} as the {@link #getBlock(int, int)} method
     * migt
     * createBlock
     * and store a new air block at the given location
     *
     * @param worldLoc
     *     The world location to check
     *
     * @return If the block at the given location is air.
     */
    public boolean isAir(@NotNull Location worldLoc) {return isAir(worldLoc.x, worldLoc.y);}

    /**
     * Check if a given location in the world is {@link Material#AIR} (or internally, doesn't exists) this is faster
     * than a
     * standard {@code getBlock(worldX, worldY).getMaterial == Material.AIR} as the {@link #getBlock(int, int)} method
     * migt
     * createBlock
     * and store a new air block at the given location
     *
     * @param worldX
     *     The x coordinate from world view
     * @param worldY
     *     The y coordinate from world view
     *
     * @return If the block at the given location is air.
     */
    public boolean isAir(int worldX, int worldY) {
        int chunkX = CoordUtil.worldToChunk(worldX);
        int chunkY = CoordUtil.worldToChunk(worldY);

        int localX = worldX - chunkX * Chunk.CHUNK_SIZE;
        int localY = worldY - chunkY * Chunk.CHUNK_SIZE;

        Block b = getChunk(chunkX, chunkY).getBlocks()[localX][localY];
        return b == null || b.getMaterial() == Material.AIR;
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
            Block rel = getRawBlock(worldX + dir.dx, worldY + dir.dy);
            if (rel instanceof UpdatableBlock) {
                ((UpdatableBlock) rel).setUpdate(true);
            }
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
        return chunks.containsKey(chunkLoc);
    }

    /**
     * @param chunk
     *     The chunk to unload
     *
     * @return If the chunk was unloaded
     */
    public void unload(@Nullable Chunk chunk) {
        if (chunk != null && chunk.isLoaded() && chunk.isAllowingUnloading()) {
            chunkLoader.save(chunk);
            chunk.dispose();
        }
    }

    /**
     * @param worldLoc
     *     The world location of this chunk
     *
     * @return The chunk at the given world location
     */
    @NotNull
    public Chunk getChunkFromWorld(@NotNull Location worldLoc) {
        return getChunk(CoordUtil.worldToChunk(worldLoc));
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
    public String getName() {
        return name;
    }

    /**
     * @return Unique identification of this world
     */
    public UUID getUuid() {
        return uuid;
    }


    public void setName(String name) {
        this.name = name;
    }

    @NotNull
    public WorldRender getRender() {
        return render;
    }

    @Nullable
    public WorldInputHandler getInput() {
        return input;
    }

    @NotNull
    public WorldTicker getWorldTicker() {
        return ticker;
    }

    /**
     * @return The current world tick
     */
    public long getTick() {
        return ticker.getTickId();
    }

    @Override
    public String toString() {
        return "World{" + "name='" + name + '\'' + ", uuid=" + uuid + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        World world = (World) o;
        return Objects.equals(uuid, world.uuid);
    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : 0;
    }

    @Override
    public void dispose() {
        render.dispose();
        ticker.stop();
        if (input != null) { input.dispose(); }
    }

    /**
     * @return The current folder of the world or {@code null} if no disk should be used
     */
    @Nullable
    public FileHandle worldFolder() {
        if (Main.renderGraphic) {
            if (worldFile == null) {
                worldFile = Gdx.files.external(Main.WORLD_FOLDER + uuid);
            }
            return worldFile;
        }
        else {
            return null;
        }
    }

    public void save() {
        if (!Main.loadWorldFromDisk) {
            return;
        }
        FileHandle worldFolder = worldFolder();
        if (worldFolder == null) { return; }
        for (Chunk chunk : chunks.values()) {
            chunkLoader.save(chunk);
        }
        FileHandle worldZip = worldFolder.parent().child(uuid + ".zip");
        try {
            ZipUtils.zip(worldFolder, worldZip);
            Main.inst().getConsoleLogger().log("World saved!");
        } catch (IOException e) {
            Main.inst().getConsoleLogger()
                .log(LogLevel.ERROR, "Failed to save world due to a " + e.getClass().getSimpleName());
            e.printStackTrace();
            return;
        }

        worldFolder.deleteDirectory();
    }

    public void load() {
        if (!Main.loadWorldFromDisk) {
            return;
        }
        FileHandle worldFolder = worldFolder();
        if (worldFolder == null) { return; }
        FileHandle worldZip = worldFolder.parent().child(uuid + ".zip");
        Main.inst.getConsoleLogger().log("Loading/saving world from '" + worldZip.file().getAbsolutePath() + '\'');
        if (!worldZip.exists()) {
            Main.inst().getConsoleLogger().log("No world save found");
            return;
        }

        worldFolder.deleteDirectory();
        ZipUtils.unzip(worldFolder, worldZip);
    }

    @Override
    public void updateRare() {
        for (Chunk chunk : chunks.values()) {
            chunk.updateRare();
        }
        for (Entity entity : entities) {
            entity.updateRare();
        }
        getRender().updateRare();
    }

    @Override
    public void update() {

        updatePhysics();

        long tick = getWorldTicker().getTickId();
        for (Iterator<Chunk> iterator = chunks.values().iterator(); iterator.hasNext(); ) {
            Chunk chunk = iterator.next();

            //clean up dead chunks
            if (!chunk.isLoaded()) {
                iterator.remove();
                continue;
            }
            //Unload chunks not seen for 5 seconds
            if (chunk.isAllowingUnloading() && getRender().isOutOfView(chunk) &&
                tick - chunk.getLastViewedTick() > Chunk.CHUNK_UNLOAD_TIME) {
                unload(chunk);
                iterator.remove();
                continue;
            }
            chunk.update();
        }
        //noinspection LibGDXUnsafeIterator
        for (Entity entity : entities) {
            entity.update();
        }
    }

    /**
     * @return the current entities
     */
    public Set<Entity> getEntities() {
        return entities;
    }

    public Set<Player> getPlayers() {
        return players;
    }

    /**
     * Add the given entity to entities in the world.
     * <b>NOTE</b> this is automatically done when creating a new entity instance. Do not use this method
     *
     * @param entity
     *     The entity to add
     */
    public void addEntity(@NotNull Entity entity) {
        entities.add(entity);
        if (entity instanceof Player) {
            players.add((Player) entity);
        }
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

    public Array<Entity> getEntities(float worldX, float worldY) {
        Array<Entity> entities = new Array<>(false, 5);
        for (Entity entity : this.entities) {
            Vector2 pos = entity.getPosition();
            if (Util.isBetween(pos.x - entity.getHalfBox2dWidth(), worldX, pos.x + entity.getHalfBox2dWidth()) && //
                Util.isBetween(pos.y - entity.getHalfBox2dHeight(), worldY, pos.y + entity.getHalfBox2dHeight())) {

                entities.add(entity);
            }
        }
        return entities;
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
        Block block = getRawBlock(worldX, worldY);
        if (block == null) {
            //noinspection LibGDXUnsafeIterator
            for (Entity entity : getEntities(worldX, worldY)) {
                if (entity instanceof MaterialEntity) {
                    return ((MaterialEntity) entity).getMaterial();
                }
            }

            return Material.AIR;
        }
        return block.getMaterial();
    }


    /**
     * Remove and disposes the given entity
     *
     * @param entity
     */
    public void removeEntity(@NotNull Entity entity) {
        entities.remove(entity);
        if (entity instanceof Player) {
            players.remove(entity);
        }
        entity.dispose();
    }

    public Collection<Chunk> getLoadedChunks() {
        return chunks.values();
    }

    public ChunkLoader getChunkLoader() {
        return chunkLoader;
    }

    @Override
    public void resize(int width, int height) {
        if (Main.renderGraphic) {
            render.resize(width, height);
            input.resize(width, height);
        }
    }

    public com.badlogic.gdx.physics.box2d.World getBox2dWorld() {
        return box2dWorld;
    }

    public void updatePhysics() {
        if (dayTicking && getTick() % (WorldTicker.TICKS_PER_SECOND * 5) == 0) {
            if (time > MAX_DEG_SKYLIGHT) {
                time = MIN_DEG_SKYLIGHT;
            }
            else if (time < MIN_DEG_SKYLIGHT) {
                time = MIN_DEG_SKYLIGHT;
            }
            getRender().getSkylight().setDirection(++time);
        }

        synchronized (WorldRender.BOX2D_LOCK) {
            getBox2dWorld().step(WorldTicker.SECONDS_DELAY_BETWEEN_TICKS, 6, 2);

            if (Main.renderGraphic && WorldRender.lights) {
                synchronized (WorldRender.LIGHT_LOCK) {
                    getRender().getRayHandler().update();
                }
            }
        }
    }
}
