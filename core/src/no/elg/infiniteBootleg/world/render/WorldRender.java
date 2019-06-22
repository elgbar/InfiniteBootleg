package no.elg.infiniteBootleg.world.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;

/**
 * @author Elg
 */
public class WorldRender implements Updatable, Renderer, Disposable {

    public static final int VERT_START = 0;
    public static final int VERT_END = 1;
    public static final int HOR_START = 2;
    public static final int HOR_END = 3;

    public final static int CHUNK_TEXTURE_WIDTH = CHUNK_SIZE * BLOCK_SIZE;
    public final static int CHUNK_TEXTURE_HEIGHT = CHUNK_SIZE * BLOCK_SIZE;

    private final World world;
    private EntityRenderer entityRenderer;
    private SpriteBatch batch;

    private OrthographicCamera camera;
    private final Rectangle viewBound;
    private final int[] chunksInView;
    private ChunkRenderer chunkRenderer;

    public WorldRender(@NotNull World world) {
        viewBound = new Rectangle();
        chunksInView = new int[4];
        this.world = world;
        if (Main.renderGraphic) {

            chunkRenderer = new ChunkRenderer(this);
            entityRenderer = new EntityRenderer(this);

            camera = new OrthographicCamera();
            camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch = new SpriteBatch();
            batch.setProjectionMatrix(camera.combined);

        }
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
        viewBound.set(camera.position.x - w / 2, camera.position.y - h / 2, w, h);

        chunksInView[HOR_START] = (int) Math.floor(viewBound.x / CHUNK_TEXTURE_WIDTH);
        chunksInView[HOR_END] = (int) Math.floor((viewBound.x + viewBound.width + CHUNK_TEXTURE_WIDTH) / CHUNK_TEXTURE_WIDTH);

        chunksInView[VERT_START] = (int) Math.floor(viewBound.y / CHUNK_TEXTURE_HEIGHT);
        chunksInView[VERT_END] = (int) Math.floor((viewBound.y + viewBound.height + CHUNK_TEXTURE_HEIGHT) / CHUNK_TEXTURE_HEIGHT);
    }

    @Override
    public void render() {
        chunkRenderer.render();
        if (Gdx.graphics.getDeltaTime() < 0.05) {
            //only render more chunks when the computer isn't struggling with the rendering
            chunkRenderer.render();
            chunkRenderer.render();
            chunkRenderer.render();
            chunkRenderer.render();
            chunkRenderer.render();
            chunkRenderer.render();
            chunkRenderer.render();
            chunkRenderer.render();
            chunkRenderer.render();
        }

        final int colEnd = chunksInView[HOR_END];
        final int colStart = chunksInView[HOR_START];
        final int rowEnd = chunksInView[VERT_END];
        final int rowStart = chunksInView[VERT_START];

        //set to 1 to debug what chunks are rendered
        final int debug = 0;

        batch.begin();
        for (int y = rowStart + debug; y < rowEnd - debug; y++) {
            for (int x = colStart + debug; x < colEnd - debug; x++) {
                Chunk chunk = world.getChunk(x, y);
                TextureRegion textureRegion = chunk.getTexture(); //get texture here to update last viewed in chunk
                if (chunk.isAllAir()) {
                    continue;
                }
                if (textureRegion == null) {
                    //if it somehow failed to render the first time, make sure it is up to date now
                    chunkRenderer.queueRendering(chunk, false);
                    continue;
                }
                float dx = chunk.getLocation().x * CHUNK_TEXTURE_WIDTH;
                float dy = chunk.getLocation().y * CHUNK_TEXTURE_HEIGHT;
                batch.draw(textureRegion, dx, dy, CHUNK_TEXTURE_WIDTH, CHUNK_TEXTURE_HEIGHT);
            }
        }
        entityRenderer.render();
        batch.end();
    }

    public boolean inInView(@NotNull Chunk chunk) {
        Location pos = chunk.getLocation();
        return pos.x >= chunksInView[HOR_START] && pos.x < chunksInView[HOR_END] && pos.y >= chunksInView[VERT_START] &&
               pos.y < chunksInView[VERT_END];
    }

    public int[] getChunksInView() {
        return chunksInView;
    }

    public Rectangle getViewBound() {
        return viewBound;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public World getWorld() {
        return world;
    }

    public ChunkRenderer getChunkRenderer() {
        return chunkRenderer;
    }

    public EntityRenderer getEntityRenderer() {
        return entityRenderer;
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    @Override
    public void dispose() {
        batch.dispose();
        chunkRenderer.dispose();
    }
}
