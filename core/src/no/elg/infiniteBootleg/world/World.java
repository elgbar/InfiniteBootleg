package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectSet;
import com.google.common.base.Preconditions;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.input.WorldInputHandler;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.util.Resizable;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.util.ZipUtils;
import no.elg.infiniteBootleg.world.blocks.UpdatableBlock;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import no.elg.infiniteBootleg.world.loader.ChunkLoader;
import no.elg.infiniteBootleg.world.render.HeadlessWorldRenderer;
import no.elg.infiniteBootleg.world.render.Ticking;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import no.elg.infiniteBootleg.world.subgrid.MaterialEntity;
import no.elg.infiniteBootleg.world.subgrid.Removable;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import org.jetbrains.annotations.Contract;
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
public class World implements Disposable, Ticking, Resizable {

    public static final short GROUND_CATEGORY = 0x1;
    public static final short LIGHTS_CATEGORY = 0x2;
    public static final short ENTITY_CATEGORY = 0x4;
    public static final short GROUND_ENTITY_CATEGORY = 0x8;

    public static final Filter BLOCK_ENTITY_FILTER;
    public static final Filter SOLID_TRANSPARENT_FILTER;
    public static final Filter ENTITY_FILTER;
    public static final Filter LIGHT_FILTER;

    public static final int SKYLIGHT_SHADOW_LENGTH = 2;
    /**
     * How many degrees the time light should have before triggering sunset/sunrise. This will happen from {@code
     * -TWILIGHT_DEGREES} to {@code +TWILIGHT_DEGREES}
     */
    public static final float TWILIGHT_DEGREES = 20;

    public static final float SUNRISE_TIME = 0;
    public static final float MIDDAY_TIME = -90;
    public static final float SUNSET_TIME = -180 + TWILIGHT_DEGREES;
    public static final float MIDNIGHT_TIME = -270;

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
    private FileHandle worldFile;

    //only exists when graphics exits
    private WorldInputHandler input;
    private WorldRender render;
    @NotNull
    private final WorldBody worldBody;

    private Set<Entity> entities; //all entities in this world (including living entities)
    private Set<LivingEntity> livingEntities; //all player in this world

    private String name = "World";
    private float time;
    private float timeScale = 1;
    private final Color baseColor = new Color(Color.WHITE);
    private final Color tmpColor = new Color();

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
        livingEntities = ConcurrentHashMap.newKeySet();
        livingEntities = ConcurrentHashMap.newKeySet();
        time = MIDDAY_TIME;

        byte[] UUIDSeed = new byte[128];
        MathUtils.random.nextBytes(UUIDSeed);
        uuid = UUID.nameUUIDFromBytes(UUIDSeed);

        chunkLoader = new ChunkLoader(this, generator);
        ticker = new WorldTicker(this);
        worldBody = new WorldBody(this);

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

        if (raw) { return getChunk(chunkX, chunkY).getBlocks()[localX][localY]; }
        else { return getChunk(chunkX, chunkY).getBlock(localX, localY); }
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
     * standard {@code getBlock(worldX, worldY).getMaterial == Material.AIR} as the {@link #getBlock(int, int, boolean)}
     * method
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
     * standard {@code getBlock(worldX, worldY).getMaterial == Material.AIR} as the {@link #getBlock(int, int, boolean)}
     * method
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
            Block rel = getBlock(worldX + dir.dx, worldY + dir.dy, true);
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
            Main.inst().getConsoleLogger().log(LogLevel.ERROR,
                                               "Failed to save world due to a " + e.getClass().getSimpleName());
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
    public void tickRare() {
        for (Chunk chunk : chunks.values()) {
            chunk.tickRare();
        }
        for (Entity entity : entities) {
            entity.tickRare();
        }
    }

    public float getSkyBrightness() {return getSkyBrightness(time);}

    /**
    public float getSkyBrightness(float time) {
        float dir;
        if (time >= 0) {
            dir = time % 360;
        }
        else {
            int mult = (int) (-time / 360) + 1;
            dir = mult * 360 + time;
        }
        float gray = 0;

        if (dir <= 180) {
            return 0;
        }
        else if (dir == 0) {
            gray = 0.5f;
        }
        else if (dir > 360 - World.TWILIGHT_DEGREES && dir < 360) {
            gray = (360 - dir) / (World.TWILIGHT_DEGREES);
        }
        else if (dir >= 180 && dir <= 180 + World.TWILIGHT_DEGREES) {
            gray = ((dir - 180) / (World.TWILIGHT_DEGREES));
        }
        else if (dir > 180) {
            gray = 1; //white
        }
        return gray;
    }

    @Override
    public void tick() {
        //tick all box2d elements
        worldBody.tick();
        WorldRender wr = getRender();

        //update light direction
        if (dayTicking) {
            time -= WorldTicker.SECONDS_DELAY_BETWEEN_TICKS * timeScale;
            wr.getSkylight().setDirection(time);
        }

        //update lights
        if (Main.renderGraphic && WorldRender.lights && ticker.getTickId() % 3 == 0) {
            synchronized (WorldRender.BOX2D_LOCK) {
                synchronized (WorldRender.LIGHT_LOCK) {
                    float brightness = getSkyBrightness(time);
                    if (brightness > 0) {
                        wr.getSkylight().setColor(tmpColor.set(baseColor).mul(brightness, brightness, brightness, 1));
                    }
                    else { wr.getSkylight().setColor(Color.BLACK); }
                    wr.getRayHandler().update();
                }
            }
        }

        //tick all chunks and blocks in chunks
        long tick = getWorldTicker().getTickId();
        for (Iterator<Chunk> iterator = chunks.values().iterator(); iterator.hasNext(); ) {
            Chunk chunk = iterator.next();

            //clean up dead chunks
            if (!chunk.isLoaded()) {
                iterator.remove();
                continue;
            }
            //Unload chunks not seen for 5 seconds
            if (chunk.isAllowingUnloading() && wr.isOutOfView(chunk) &&
                tick - chunk.getLastViewedTick() > Chunk.CHUNK_UNLOAD_TIME) {
                unload(chunk);
                iterator.remove();
                continue;
            }
            chunk.tick();
        }

        //tick all entities
        for (Entity entity : entities) {
            entity.tick();
        }
    }

    /**
     * @return the current entities
     */
    public Set<Entity> getEntities() {
        return entities;
    }

    public Set<LivingEntity> getLivingEntities() {
        return livingEntities;
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
            livingEntities.add((Player) entity);
        }
    }

    public ObjectSet<Block> getBlocksAABB(float worldX, float worldY, float offsetX, float offsetY, boolean raw) {
        ObjectSet<Block> blocks = new ObjectSet<>();
        int x = MathUtils.floor(worldX - offsetX);
        float maxX = worldX + offsetX;
        for (; x <= maxX; x++) {
            int y = MathUtils.floor(worldY - offsetY);
            float maxY = worldY + offsetY;
            for (; y <= maxY; y++) {
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
            if (Math.abs(Vector2.dst2(worldX, worldY, block.getWorldX() + 0.5f, block.getWorldY() + 0.5f)) <=
                radiusSquare) {
                blocks.add(block);
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
        Block block = getBlock(worldX, worldY, true);
        if (block == null) {
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
            livingEntities.remove(entity);
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
        }
    }

    @NotNull
    public WorldBody getWorldBody() {
        return worldBody;
    }

    public float getTimeScale() {
        return timeScale;
    }

    public void setTimeScale(float timeScale) {
        this.timeScale = timeScale;
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    public Color getBaseColor() {
        return baseColor;
    }
}
