package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Elg
 */
public class World {

    public final static int BLOCK_SIZE = 16;

    private final ChunkGenerator generator;
    private final long seed;
    private final Random random;
    private final Map<Location, Chunk> chunks;
    private final SpriteBatch batch;

    private String name = "World";
    private final UUID uuid;

    private final OrthographicCamera camera;
    private final Rectangle viewBounds;

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
        chunks = new ConcurrentHashMap<>();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        uuid = UUID.randomUUID();
        batch = new SpriteBatch();


        this.viewBounds = new Rectangle();
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

    @NotNull
    public Chunk getChunkFromWorld(@NotNull Location location) {
        return getChunkFromWorld(location.x, location.y);
    }

    public long getSeed() {
        return seed;
    }

    private void setView() {
        batch.setProjectionMatrix(camera.combined);
        float width = camera.viewportWidth * camera.zoom;
        float height = camera.viewportHeight * camera.zoom;
        float w = width * Math.abs(camera.up.y) + height * Math.abs(camera.up.x);
        float h = height * Math.abs(camera.up.y) + width * Math.abs(camera.up.x);
        viewBounds.set(camera.position.x - w / 2, camera.position.y - h / 2, w, h);
    }

    public void render() {
        camera.update(); //maybe not call every time we render...
        setView();

//        float centerX = camera.position.x / 2;
//        float centerY = camera.position.y / 2;
//        float blocksHor = centerX / BLOCK_SIZE;
//        float blocksVer = centerY / BLOCK_SIZE;
//        System.out.println("blocksVer = " + blocksVer);
//        System.out.println("blocksHor = " + blocksHor);

        final Vector3 unproject = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
//        final Vector3 unproject = camera.unproject(new Vector3(centerX, centerY, 0));

        final int blockX = (int) (unproject.x / BLOCK_SIZE);
        final int blockY = (int) (unproject.y / BLOCK_SIZE);

        final int col1 = (int) (viewBounds.x / Chunk.CHUNK_WIDTH);
        final int col2 = (int) ((viewBounds.x + viewBounds.width + Chunk.CHUNK_WIDTH) / Chunk.CHUNK_WIDTH);

        final int row1 = (int) (viewBounds.y / Chunk.CHUNK_HEIGHT);
        final int row2 = (int) ((viewBounds.y + viewBounds.height + Chunk.CHUNK_HEIGHT) / Chunk.CHUNK_HEIGHT);


//        float y = row2 * World.BLOCK_SIZE;
//        float xStart = col1 * World.BLOCK_SIZE;
//        float size = World.BLOCK_SIZE * camera.zoom; //size of each tile

        batch.begin();
//        for (int row = row2; row >= row1; row--) {
////            float x = xStart;
//            for (int col = col1; col < col2; col++) {
//                System.out.print("col = " + col);
//                System.out.println(" row = " + row);
        Chunk chunk = getChunkFromWorld(blockX, blockY);
        Location chunkLoc = chunk.getChunkPos().mult(Chunk.CHUNK_WIDTH, Chunk.CHUNK_HEIGHT);

        for (Block block : chunk) {

            //camera.position.x + chunkLoc.x * blkLoc.x * size
            Location blkLoc = block.getLocation();
            float x = (blkLoc.x + chunkLoc.x) * World.BLOCK_SIZE;
            float y = (blkLoc.y + chunkLoc.y) * World.BLOCK_SIZE;
            batch.draw(block.getTexture(), x, y, World.BLOCK_SIZE, World.BLOCK_SIZE);
        }
//            }
//        }
        batch.end();

        //the chunk we're standing in

        if (Gdx.graphics.getFrameId() % (Gdx.graphics.getFramesPerSecond() + 1) / 2 == 0) {
            System.out.println("unproject = " + unproject);
            System.out.println("zoom = " + camera.zoom);
            System.out.println("cam  = " + camera.position);
            System.out.println("blockX = " + blockX);
            System.out.println("blockY = " + blockY);
            System.out.println("chunk = " + chunk);
            System.out.println("\n");
        }

//
//        for (int i = 0; i < ; i++) {
//
//        }


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
}
