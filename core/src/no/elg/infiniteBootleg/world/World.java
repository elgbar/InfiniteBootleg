package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.Disposable;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.input.WorldInputHandler;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.util.ZipUtils;
import no.elg.infiniteBootleg.world.blocks.UpdatableBlock;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import no.elg.infiniteBootleg.world.loader.ChunkLoader;
import no.elg.infiniteBootleg.world.render.HeadlessWorldRenderer;
import no.elg.infiniteBootleg.world.render.Updatable;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Elg
 */
public class World implements Disposable, Updatable {

    public static final short GROUND_CATEGORY = 0b001;
    public static final short LIGHT_CATEGORY = 0b010;
    public static final short ENTITY_CATEGORY = 0b100;

    public static final Filter ENTITY_FILTER;
    public static final Filter LIGHT_FILTER;

    static {
        ENTITY_FILTER = new Filter();
        ENTITY_FILTER.categoryBits = ENTITY_CATEGORY;
        ENTITY_FILTER.maskBits = ENTITY_CATEGORY | GROUND_CATEGORY;

        LIGHT_FILTER = new Filter();
        LIGHT_FILTER.categoryBits = LIGHT_CATEGORY;
        LIGHT_FILTER.maskBits = GROUND_CATEGORY;
    }

    private final long seed;
    private final Map<Location, Chunk> chunks;
    private final WorldTicker ticker;
    private final ChunkLoader chunkLoader;
    private FileHandle worldFile;

    //only exists when graphics exits
    private WorldInputHandler input;
    private WorldRender render;

    private String name = "World";
    private final UUID uuid;
    private Set<Entity> entities;


    /**
     * Generate a world with a random seed
     *
     * @param generator
     */
    public World(@NotNull ChunkGenerator generator) {
        this(generator, new Random().nextLong());
    }

    public World(@NotNull ChunkGenerator generator, long seed) {
        this.seed = seed;
        MathUtils.random.setSeed(seed);
        chunks = new ConcurrentHashMap<>();
        entities = ConcurrentHashMap.newKeySet();

        byte[] UUIDSeed = new byte[128];
        MathUtils.random.nextBytes(UUIDSeed);
        uuid = UUID.nameUUIDFromBytes(UUIDSeed);

        if (Main.renderGraphic) {
            render = new WorldRender(this);
            input = new WorldInputHandler(render);

            new Player(this);
        }
        else {
            render = new HeadlessWorldRenderer(this);
        }

        chunkLoader = new ChunkLoader(this, generator);
        ticker = new WorldTicker(this);
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
            chunk = chunkLoader.load(chunkLoc);
            chunks.put(chunkLoc, chunk);
        }
//        System.out.println("chunk.isLoaded() = " + chunk.isLoaded());
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

        int localX = worldX - chunkX * Chunk.CHUNK_SIZE;
        int localY = worldY - chunkY * Chunk.CHUNK_SIZE;

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
     * Check if a given location in the world is {@link Material#AIR} (or internally, doesn't exists) this is faster than a
     * standard {@code getBlock(worldX, worldY).getMaterial == Material.AIR} as the {@link #getBlock(int, int)} method migt create
     * and store a new air block at the given location
     *
     * @param worldLoc
     *     The world location to check
     *
     * @return If the block at the given location is air.
     */
    public boolean isAir(@NotNull Location worldLoc) {return isAir(worldLoc.x, worldLoc.y);}

    /**
     * Check if a given location in the world is {@link Material#AIR} (or internally, doesn't exists) this is faster than a
     * standard {@code getBlock(worldX, worldY).getMaterial == Material.AIR} as the {@link #getBlock(int, int)} method migt create
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
     * Set all blocks around a given block to be updated
     *
     * @param worldLoc
     *     The coordinates to updates around (but not included)
     */
    public void updateBlocksAround(@NotNull Location worldLoc) {
        updateBlocksAround(worldLoc.x, worldLoc.y);
    }

    /**
     * Set all blocks around a given block to be updated. Given location not included
     *
     * @param worldX
     *     The x coordinate from world view
     * @param worldY
     *     The y coordinate from world view
     */
    public void updateBlocksAround(int worldX, int worldY) {
        Block center = getBlock(worldX, worldY);
        for (Direction dir : Direction.values()) {
            Block rel = center.getRelative(dir);
            if (rel instanceof UpdatableBlock) {
                ((UpdatableBlock) rel).setUpdate(true);
            }
        }
    }

    /**
     * @return If the given chunk is loaded in memory
     */
    public boolean isChunkLoaded(int chunkX, int chunkY) {return isChunkLoaded(new Location(chunkX, chunkY));}

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
    public boolean unload(@Nullable Chunk chunk) {
        if (chunk == null || !chunk.isLoaded() || !isChunkLoaded(chunk.getLocation())) {
            return false;
        }
        chunk.dispose();
        chunkLoader.save(chunk);
        return chunk.unload();
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
            Main.inst().getConsoleLogger().log(LogLevel.ERROR, "Failed to save world due to a " + e.getClass().getSimpleName());
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
    public void update() {

        getRender().updatePhysics();

        long tick = getWorldTicker().getTickId();
        for (Iterator<Chunk> iterator = chunks.values().iterator(); iterator.hasNext(); ) {
            Chunk chunk = iterator.next();

            //clean up dead chunks
            if (!chunk.isLoaded()) {
                Main.inst().getConsoleLogger().log("Chunk unloaded, but not removed from chunks");
                iterator.remove();
                continue;
            }


//            System.out.println("chunks.size() = " + chunks.size());

//            System.out.println("delta last view @ " + chunk.getLocation() + " (all air? " + chunk.isAllAir() + "): " +
//                               (tick - chunk.getLastViewedTick()));

//            System.out.println("chunk.isAllAir() (" + chunk.getLocation() + ") = " + chunk.isAllAir());

            //Unload chunks not seen for 5 seconds
            if (chunk.allowUnload() && getRender().isOutOfView(chunk) &&
                tick - chunk.getLastViewedTick() > Chunk.CHUNK_UNLOAD_TIME) {
                System.out.println("unloaded chunk " + chunk.getLocation());
                unload(chunk);
                iterator.remove();
                continue;
            }
            chunk.update();
        }
        for (Entity entity : entities) {
            entity.update();
        }
    }

    /**
     * @return unmodifiable view of the current entities
     */
    public Set<Entity> getEntities() {
        return Collections.unmodifiableSet(entities);
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
    }

    /**
     * Remove and disposes the given entity
     *
     * @param entity
     */
    public void removeEntity(@NotNull Entity entity) {
        entities.remove(entity);
        entity.dispose();
    }

    public Collection<Chunk> getLoadedChunks() {
        return chunks.values();
    }

    public ChunkLoader getChunkLoader() {
        return chunkLoader;
    }
}
