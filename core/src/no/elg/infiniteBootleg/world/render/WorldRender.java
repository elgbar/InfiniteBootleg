package no.elg.infiniteBootleg.world.render;

import box2dLight.DirectionalLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.util.Resizable;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_TEXTURE_SIZE;

/**
 * @author Elg
 */
public class WorldRender implements Updatable, Renderer, Disposable, Resizable {

    public static final float MIN_ZOOM = 0.25f;
    public static final float AMBIENT_LIGHT = 0.026f;
    public static final int RAYS_PER_BLOCK = 200;
    /**
     * How many chunks to the sides extra should be included.
     * <p>
     * One of the these chunks comes from the need that the player should not see that a chunks loads in.
     */
    public static final int CHUNKS_IN_VIEW_HORIZONTAL_RENDER = 1;
    /**
     * How many chunks to the sides extra should be included.
     * <p>
     * This is the for light during twilight to stop light bleeding into the sides of the screen when moving.
     */
    public static final int CHUNKS_IN_VIEW_HORIZONTAL_PHYSICS = CHUNKS_IN_VIEW_HORIZONTAL_RENDER + 1;
    //add one to make sure we are always in darkness underground
    public static final int CHUNKS_IN_VIEW_TOP_VERTICAL_OFFSET = 1;

    /**
     * How much must the player zoom to trigger a skylight reset
     *
     * @see #resetSkylight()
     */
    public static final float SKYLIGHT_ZOOM_THRESHOLD = 0.25f;
    /**
     * How many {@link Graphics#getFramesPerSecond()} should there be when rendering multiple chunks
     */
    public static final int FPS_FAST_CHUNK_RENDER_THRESHOLD = 10;


    public final World world;
    private RayHandler rayHandler;
    private EntityRenderer entityRenderer;
    private SpriteBatch batch;

    private OrthographicCamera camera;
    private final Rectangle viewBound;
    private final ChunkViewed chunksInView;
    private ChunkRenderer chunkRenderer;

    private Box2DDebugRenderer box2DDebugRenderer;
    private DebugChunkRenderer chunkDebugRenderer;

    private Matrix4 m4 = new Matrix4();
    private DirectionalLight skylight;
    private float lastZoom;
    Map<Chunk, TextureRegion> draw = new HashMap<>();

    public static boolean lights = true;
    public static boolean debugBox2d = false;

    public final static Object LIGHT_LOCK = new Object();
    public final static Object BOX2D_LOCK = new Object();

    public final static class ChunkViewed {

        private ChunkViewed() {}

        public int horizontal_start;
        public int horizontal_end;
        public int vertical_start;
        public int vertical_end;

        public int getHorizontalLength() {
            return horizontal_end - horizontal_start;
        }

        public int getVerticalLength() {
            return vertical_end - vertical_start;
        }
    }

    public WorldRender(@NotNull World world) {
        viewBound = new Rectangle();
        chunksInView = new ChunkViewed();
        this.world = world;

        if (Main.renderGraphic) {
            chunkRenderer = new ChunkRenderer(this);
            entityRenderer = new EntityRenderer(this);

            camera = new OrthographicCamera();
            camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            camera.zoom = 1f;
            camera.position.x = 0;
            camera.position.y = 0;

            batch = new SpriteBatch();
            batch.setProjectionMatrix(camera.combined);

            chunkDebugRenderer = new DebugChunkRenderer(this);
            box2DDebugRenderer = new Box2DDebugRenderer();

            RayHandler.setGammaCorrection(true);
            RayHandler.useDiffuseLight(true);
            rayHandler = new RayHandler(world.getWorldBody().getBox2dWorld(), 200, 140);
            rayHandler.setBlurNum(2);
            rayHandler.setAmbientLight(AMBIENT_LIGHT, AMBIENT_LIGHT, AMBIENT_LIGHT, 1);
            resetSkylight();
        }
    }

    public void resetSkylight() {
        synchronized (BOX2D_LOCK) {
            synchronized (LIGHT_LOCK) {
                if (skylight != null) {
                    skylight.remove();
                }
                skylight = new DirectionalLight(rayHandler, blocksHorizontally() * RAYS_PER_BLOCK, Color.WHITE,
                                                World.SUNRISE_TIME);
                skylight.setStaticLight(true);
                skylight.setContactFilter(World.LIGHT_FILTER);
                skylight.setSoftnessLength(World.SKYLIGHT_SOFTNESS_LENGTH); //restore lights 1.4 functionality
            }
        }
    }

    @Override
    public void update() {
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        m4.set(camera.combined).scl(Block.BLOCK_SIZE);

        final float width = camera.viewportWidth * camera.zoom;
        final float height = camera.viewportHeight * camera.zoom;

        if (lights) {
            rayHandler.setCombinedMatrix(m4, 0, 0, width, height);
        }

        if (!getWorld().getWorldTicker().isPaused()) {

            float w = width * Math.abs(camera.up.y) + height * Math.abs(camera.up.x);
            float h = height * Math.abs(camera.up.y) + width * Math.abs(camera.up.x);
            viewBound.set(camera.position.x - w / 2, camera.position.y - h / 2, w, h);

            chunksInView.horizontal_start = //
                MathUtils.floor(viewBound.x / CHUNK_TEXTURE_SIZE) - CHUNKS_IN_VIEW_HORIZONTAL_PHYSICS;
            chunksInView.horizontal_end = //
                MathUtils.floor((viewBound.x + viewBound.width + CHUNK_TEXTURE_SIZE) / CHUNK_TEXTURE_SIZE) +
                CHUNKS_IN_VIEW_HORIZONTAL_PHYSICS;

            chunksInView.vertical_start = MathUtils.floor(viewBound.y / CHUNK_TEXTURE_SIZE);
            chunksInView.vertical_end = //
                MathUtils.floor((viewBound.y + viewBound.height + CHUNK_TEXTURE_SIZE) / CHUNK_TEXTURE_SIZE) +
                CHUNKS_IN_VIEW_TOP_VERTICAL_OFFSET;

            if (Math.abs(lastZoom - camera.zoom) > SKYLIGHT_ZOOM_THRESHOLD) {
                lastZoom = camera.zoom;
                resetSkylight();
            }
        }
    }

    @Override
    public void render() {
        chunkRenderer.render();
        if (Gdx.graphics.getFramesPerSecond() > FPS_FAST_CHUNK_RENDER_THRESHOLD) {
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

        draw.clear();

        for (int y = chunksInView.vertical_start; y < chunksInView.vertical_end; y++) {
            for (int x = chunksInView.horizontal_start; x < chunksInView.horizontal_end; x++) {
                Chunk chunk = world.getChunk(x, y);
                chunk.view();

                if (chunk.isDirty()) {
                    //noinspection LibGDXFlushInsideLoop
                    chunk.updateTextureNow();
                }

                if (y == chunksInView.vertical_end - 1 || x == chunksInView.horizontal_start ||
                    x == chunksInView.horizontal_end - 1) {
                    continue;
                }

                //noinspection LibGDXFlushInsideLoop
                if (chunk.isAllAir()) {
                    continue;
                }

                //get texture here to update last viewed in chunk
                //noinspection LibGDXFlushInsideLoop
                TextureRegion textureRegion = chunk.getTextureRegion();
                if (textureRegion == null) {
                    continue;
                }
                draw.put(chunk, textureRegion);
            }
        }
        batch.begin();
        for (Map.Entry<Chunk, TextureRegion> entry : draw.entrySet()) {
            float dx = entry.getKey().getChunkX() * CHUNK_TEXTURE_SIZE;
            float dy = entry.getKey().getChunkY() * CHUNK_TEXTURE_SIZE;
            batch.draw(entry.getValue(), dx, dy, CHUNK_TEXTURE_SIZE, CHUNK_TEXTURE_SIZE);
        }
        entityRenderer.render();
        batch.end();
        if (lights) {

            synchronized (BOX2D_LOCK) {
                synchronized (LIGHT_LOCK) {
                    rayHandler.render();
                }
            }
        }
        if (debugBox2d && Main.debug) {
            synchronized (BOX2D_LOCK) {
                box2DDebugRenderer.render(world.getWorldBody().getBox2dWorld(), m4);
            }
            chunkDebugRenderer.render();
        }
    }

    /**
     * @param chunk
     *     The chunk to check
     *
     * @return {@code true} if the given chunk is outside the view of the camera
     */
    public boolean isOutOfView(@NotNull Chunk chunk) {
        return chunk.getChunkX() < chunksInView.horizontal_start || chunk.getChunkX() >= chunksInView.horizontal_end ||
               chunk.getChunkY() < chunksInView.vertical_start || chunk.getChunkY() >= chunksInView.vertical_end;
    }

    public ChunkViewed getChunksInView() {
        return chunksInView;
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

    public RayHandler getRayHandler() {
        return rayHandler;
    }

    public DirectionalLight getSkylight() {
        synchronized (LIGHT_LOCK) {
            return skylight;
        }
    }

    /**
     * @return How many blocks there currently are horizontally on screen
     */
    public int blocksHorizontally() {
        return (int) Math.ceil(camera.viewportWidth * camera.zoom / Block.BLOCK_SIZE) + 1;
    }

    @Override
    public void dispose() {
        batch.dispose();
        chunkRenderer.dispose();
        rayHandler.dispose();
    }

    @Override
    public void resize(int width, int height) {
        Vector3 old = camera.position.cpy();
        camera.setToOrtho(false, width, height);
        camera.position.set(old);
        update();
    }
}
