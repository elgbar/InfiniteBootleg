package no.elg.infiniteBootleg.world.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class WorldRender implements Renderer, Disposable {


    public static final int ROW_START = 0;
    public static final int ROW_END = 1;
    public static final int COL_START = 2;
    public static final int COL_END = 3;

    private final World world;
    private final SpriteBatch batch;

    private final OrthographicCamera camera;
    private final Rectangle viewBounds;
    private final int[] chunksInView;

    public WorldRender(@NotNull World world) {
        if (!Main.RENDER_GRAPHIC) {
            throw new IllegalStateException("Cannot render world as graphics are not enabled");
        }

        this.world = world;

        batch = new SpriteBatch();


        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        this.viewBounds = new Rectangle();
        chunksInView = new int[4];
        update();
    }


    @Override
    public void update() {
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

//        final int colEnd = getViewingChunks[COL_END];
//        final int colStart = getViewingChunks[COL_START];
//        final int rowEnd = getViewingChunks[ROW_END];
//        final int rowStart = getViewingChunks[ROW_START];
////
//        Array<Chunk> seenChunks = new Array<>(false, Math.abs((colEnd - colStart) + (rowEnd - rowStart)));
//
//        for (int row = rowStart; row < rowEnd; row++) {
//            for (int col = colStart; col < colEnd; col++) {
//                getChunk(col, row).updateTexture();
//            }
//        }
//
//        synchronized (this) {
//            chunks.entrySet().removeIf(entry -> !seenChunks.contains(entry.getValue(), false));
//        }
    }

    @Override
    public void render() {

        final int colEnd = chunksInView[COL_END];
        final int colStart = chunksInView[COL_START];
        final int rowEnd = chunksInView[ROW_END];
        final int rowStart = chunksInView[ROW_START];

        final int debug = 0;

        batch.begin();
        for (int row = rowStart + debug; row < rowEnd - debug; row++) {
            for (int col = colStart + debug; col < colEnd - debug; col++) {
                Chunk chunk = world.getChunk(col, row);
                for (Block block : chunk) {
                    if (block.getMaterial() == Material.AIR) { continue; }
                    Location blkLoc = block.getLocation();
                    float x = (blkLoc.x + chunk.getLocation().x * Chunk.CHUNK_WIDTH) * World.BLOCK_SIZE;
                    float y = (blkLoc.y + chunk.getLocation().y * Chunk.CHUNK_HEIGHT) * World.BLOCK_SIZE;
                    //noinspection ConstantConditions
                    batch.draw(block.getTexture(), x, y, World.BLOCK_SIZE, World.BLOCK_SIZE);
                }
            }
        }
        batch.end();
    }

    public int[] getChunksInView() {
        return chunksInView;
    }

    public Rectangle getViewBounds() {
        return viewBounds;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public World getWorld() {
        return world;
    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
