package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.input.WorldInputHandler;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import no.elg.infiniteBootleg.world.render.Updatable;
import no.elg.infiniteBootleg.world.render.WorldRender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Elg
 */
public class World implements Disposable, Updatable {

    public final static int BLOCK_SIZE = 16;
    public static final int CHUNK_WIDTH_SHIFT = (int) (Math.log(Chunk.CHUNK_WIDTH) / Math.log(2));
    public static final int CHUNK_HEIGHT_SHIFT = (int) (Math.log(Chunk.CHUNK_HEIGHT) / Math.log(2));

    private final ChunkGenerator generator;
    private final long seed;
    private final Random random;
    private final Map<Location, Chunk> chunks;
    private final WorldTicker ticker;

    //only exists when graphics exits
    private WorldInputHandler input;
    private WorldRender render;

    private String name = "World";
    private final UUID uuid;


    /**
     * Generate a world with a random seed
     *
     * @param generator
     */
    public World(@NotNull ChunkGenerator generator) {
        this(generator, new Random().nextLong());
    }

    public World(@NotNull ChunkGenerator generator, long seed) {
        this.generator = generator;
        this.seed = seed;
        random = new Random(seed);
        chunks = new WeakHashMap<>();

        if (Main.renderGraphic) {
            render = new WorldRender(this);
            input = new WorldInputHandler(render);
        }

        ticker = new WorldTicker(this);

        uuid = UUID.randomUUID();
    }

    @NotNull
    public Chunk getChunk(int chunkX, int chunkY) {
        return getChunk(new Location(chunkX, chunkY));
    }

    @NotNull
    public Chunk getChunk(@NotNull Location chunkLoc) {
        return chunks.computeIfAbsent(chunkLoc, loc -> generator.generate(this, loc, random));
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
    public Block getBlock(int worldX, int worldY) {

        int chunkX = CoordUtil.worldToChunk(worldX);
        int chunkY = CoordUtil.worldToChunk(worldY);

        int localX = worldX - chunkX * Chunk.CHUNK_WIDTH;
        int localY = worldY - chunkY * Chunk.CHUNK_HEIGHT;

        return getChunk(chunkX, chunkY).getBlock(localX, localY);
    }

    /**
     * @param worldX
     *     The x coordinate from world view
     * @param worldY
     *     The y coordinate from world view
     * @param material
     *     The new material to at given location
     *
     * @see Chunk#setBlock(int, int, Material, boolean)
     */
    public void setBlock(int worldX, int worldY, @Nullable Material material) {

        int chunkX = CoordUtil.worldToChunk(worldX);
        int chunkY = CoordUtil.worldToChunk(worldY);

        int localX = worldX - chunkX * Chunk.CHUNK_WIDTH;
        int localY = worldY - chunkY * Chunk.CHUNK_HEIGHT;

        getChunk(chunkX, chunkY).setBlock(localX, localY, material, true);
    }


    public boolean isLoadedAt(@NotNull Location chunkLoc) {
        return chunks.containsKey(chunkLoc);
    }

    /**
     * @param chunk
     *     The chunk to unload
     *
     * @return If the chunk was unloaded
     */
    public boolean unload(@Nullable Chunk chunk) {
        if (chunk == null || !chunk.isLoaded() || !isLoadedAt(chunk.getLocation())) {
            return false;
        }
        return chunk.unload();
    }

    @NotNull
    public Chunk getChunkFromWorld(@NotNull Location location) {
        return getChunkFromWorld(location.x, location.y);
    }

    public long getSeed() {
        return seed;
    }

    public String getName() {
        return name;
    }

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

    @NotNull
    public WorldInputHandler getInput() {
        return input;
    }

    @NotNull
    public WorldTicker getWorldTicker() {
        return ticker;
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
        input.dispose();
        ticker.dispose();
    }

    @Override
    public void update() {
        long tick = getWorldTicker().getTickId();
        for (Iterator<Chunk> iterator = chunks.values().iterator(); iterator.hasNext(); ) {
            Chunk chunk = iterator.next();

            //clean up dead chunks
            if (!chunk.isLoaded()) {
                iterator.remove();
                continue;
            }

            if (chunk.getLastViewedTick() > tick + WorldTicker.TICKS_PER_SECOND * 5) {
                System.out.println("unloaded chunk " + chunk.getLocation());
                unload(chunk);
                continue;
            }

            chunk.update();
        }
    }
}
