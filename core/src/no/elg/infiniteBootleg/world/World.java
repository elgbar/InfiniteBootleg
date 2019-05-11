package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Elg
 */
public class World extends InputAdapter implements Disposable {

    public final static int BLOCK_SIZE = 16;

    public static final int ROW_START = 0;
    public static final int ROW_END = 1;
    public static final int COL_START = 2;
    public static final int COL_END = 3;

    private final ChunkGenerator generator;
    private final long seed;
    private final Random random;
    private final Map<Location, Chunk> chunks;
    //    Array<Chunk> chunks; //all loaded chunks
    private final SpriteBatch batch;

    private String name = "World";
    private final UUID uuid;

    private final OrthographicCamera camera;
    private final Rectangle viewBounds;
    private final int[] chunksInView;

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
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        uuid = UUID.randomUUID();
        batch = new SpriteBatch();

        Main.addInputProcessor(this);

        this.viewBounds = new Rectangle();
        chunksInView = new int[4];
        updateView();

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
        int chunkX = (int) Math.floor((float) worldX / Chunk.CHUNK_WIDTH);
        int chunkY = (int) Math.floor((float) worldY / Chunk.CHUNK_HEIGHT);

        return getChunk(chunkX, chunkY);
    }

    public boolean isLoadedAt(@NotNull Location chunkLoc) {
        return chunks.containsKey(chunkLoc);
    }

    /**
     * @param chunk
     *
     * @return If the chunk was unloaded
     */
    public boolean unload(@Nullable Chunk chunk) {
        if (chunk == null || !chunk.isLoaded() || !isLoadedAt(chunk.getLocation())) {
            return false;
        }
        chunks.remove(chunk.getLocation());
        chunk.unload();
        return true;
    }

    @NotNull
    public Chunk getChunkFromWorld(@NotNull Location location) {
        return getChunkFromWorld(location.x, location.y);
    }

    public long getSeed() {
        return seed;
    }

    private void unloadOutOfView() {

    }

    public void updateView() {
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        float width = camera.viewportWidth * camera.zoom;
        float height = camera.viewportHeight * camera.zoom;
        float w = width * Math.abs(camera.up.y) + height * Math.abs(camera.up.x);
        float h = height * Math.abs(camera.up.y) + width * Math.abs(camera.up.x);
        viewBounds.set(camera.position.x - w / 2, camera.position.y - h / 2, w, h);


        float chunkTextWidth = Chunk.CHUNK_WIDTH * World.BLOCK_SIZE;
        float chunkTextHeight = Chunk.CHUNK_HEIGHT * World.BLOCK_SIZE;

        chunksInView[COL_START] = (int) Math.floor(viewBounds.x / chunkTextWidth);
        chunksInView[COL_END] = (int) Math.floor((viewBounds.x + viewBounds.width + chunkTextWidth) / chunkTextWidth);

        chunksInView[ROW_START] = (int) Math.floor(viewBounds.y / chunkTextHeight);
        chunksInView[ROW_END] = (int) Math.floor((viewBounds.y + viewBounds.height + chunkTextHeight) / chunkTextHeight);

//        final int colEnd = chunksInView[COL_END];
//        final int colStart = chunksInView[COL_START];
//        final int rowEnd = chunksInView[ROW_END];
//        final int rowStart = chunksInView[ROW_START];
//
//        Array<Chunk> seenChunks = new Array<>(false, Math.abs((colEnd - colStart) + (rowEnd - rowStart)));
//
//        for (int row = rowStart; row < rowEnd; row++) {
//            for (int col = colStart; col < colEnd; col++) {
//                seenChunks.add(getChunk(col, row));
//            }
//        }
//
//        synchronized (this) {
//            chunks.entrySet().removeIf(entry -> !seenChunks.contains(entry.getValue(), false));
//        }
    }

    public int[] chunksInView() {
        return chunksInView;
    }

    public void render() {
        final int colEnd = chunksInView[COL_END];
        final int colStart = chunksInView[COL_START];
        final int rowEnd = chunksInView[ROW_END];
        final int rowStart = chunksInView[ROW_START];
        final int debug = 0;
        batch.begin();
        for (int row = rowStart + debug; row < rowEnd - debug; row++) {
            for (int col = colStart + debug; col < colEnd - debug; col++) {
                Chunk chunk = getChunk(col, row);
                Location chunkLoc = chunk.getLocation().mult(Chunk.CHUNK_WIDTH, Chunk.CHUNK_HEIGHT);

                for (Block block : chunk) {
                    if (block == null || block.getMaterial() == Material.AIR) { continue; }
                    Location blkLoc = block.getLocation();
                    float x = (blkLoc.x + chunkLoc.x) * World.BLOCK_SIZE;
                    float y = (blkLoc.y + chunkLoc.y) * World.BLOCK_SIZE;
                    batch.draw(block.getTexture(), x, y, World.BLOCK_SIZE, World.BLOCK_SIZE);
                }
//                batch.draw();
            }
        }
        batch.end();
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        camera.position.x -= Gdx.input.getDeltaX() * camera.zoom;
        camera.position.y += Gdx.input.getDeltaY() * camera.zoom;
        updateView();
        return true;
    }

    @Override
    public boolean scrolled(int amount) {
        camera.zoom += amount * 0.05f * camera.zoom;
        if (camera.zoom <= 0) {
            camera.zoom = 0.04f;
        }
        updateView();
        return true;
    }


    public OrthographicCamera getCamera() {
        return camera;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Rectangle getViewBounds() {
        return viewBounds;
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
        Main.getInputMultiplexer().removeProcessor(this);
        batch.dispose();
    }
}
