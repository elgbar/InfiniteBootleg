package no.elg.infiniteBootleg.world.render;

import box2dLight.DirectionalLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
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

            camera.zoom = 1.5f;
            camera.position.x = 0;
            camera.position.y = 0;

            batch = new SpriteBatch();
            batch.setProjectionMatrix(camera.combined);

            chunkDebugRenderer = new DebugChunkRenderer(this);
            box2DDebugRenderer = new Box2DDebugRenderer();

            RayHandler.setGammaCorrection(true);
            RayHandler.useDiffuseLight(true);
            rayHandler = new RayHandler(world.getWorldBody().getBox2dWorld(), 1, 1);
            rayHandler.setBlurNum(2);
            rayHandler.setAmbientLight(AMBIENT_LIGHT, AMBIENT_LIGHT, AMBIENT_LIGHT, 1);

            //TODO maybe use the zoom level to get a nice number of rays? ie width*zoom*4 or something
            skylight = new DirectionalLight(rayHandler, 15000, Color.WHITE, World.MIDDAY_TIME);
            skylight.setContactFilter(World.LIGHT_FILTER);
            skylight.setStaticLight(true);
            skylight.setSoftnessLength(World.SKYLIGHT_SOFTNESS_LENGTH); //restore lights 1.4 functionality
        }
    }

    @Override
    public void update() {
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        m4.set(camera.combined).scl(Block.BLOCK_SIZE);

        if (!getWorld().getWorldTicker().isPaused()) {
            float width = camera.viewportWidth * camera.zoom;
            float height = camera.viewportHeight * camera.zoom;

            float w = width * Math.abs(camera.up.y) + height * Math.abs(camera.up.x);
            float h = height * Math.abs(camera.up.y) + width * Math.abs(camera.up.x);
            viewBound.set(camera.position.x - w / 2, camera.position.y - h / 2, w, h);

            chunksInView.horizontal_start = MathUtils.floor(viewBound.x / CHUNK_TEXTURE_SIZE) - 1;
            chunksInView.horizontal_end = MathUtils.floor(
                (viewBound.x + viewBound.width + CHUNK_TEXTURE_SIZE) / CHUNK_TEXTURE_SIZE) + 1;

            chunksInView.vertical_start = MathUtils.floor(viewBound.y / CHUNK_TEXTURE_SIZE);
            //add one to make sure we are always in darkness underground
            chunksInView.vertical_end = MathUtils.floor(
                (viewBound.y + viewBound.height + CHUNK_TEXTURE_SIZE) / CHUNK_TEXTURE_SIZE) + 1;
        }
        if (lights) {
            rayHandler.setCombinedMatrix(m4, Main.inst().getMouseBlockX(), Main.inst().getMouseBlockY(),
                                         camera.viewportWidth * camera.zoom, camera.viewportHeight * camera.zoom);
        }
    }

    Map<Chunk, TextureRegion> draw = new HashMap<>();

    @Override
    public void render() {
        chunkRenderer.render();
        if (Gdx.graphics.getDeltaTime() < 0.05f) {
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

        //set to 1 to debug what chunks are rendered
        final int debug = 0;
        draw.clear();

        int yEnd = chunksInView.vertical_end - debug;
        int xEnd = chunksInView.horizontal_end - debug;

        for (int y = chunksInView.vertical_start + debug; y < yEnd; y++) {
            for (int x = chunksInView.horizontal_start + debug; x < xEnd; x++) {
                Chunk chunk = world.getChunk(x, y);
                chunk.view();

                if (chunk.isDirty()) {
                    //noinspection LibGDXFlushInsideLoop
                    chunk.updateTextureNow();
                }

                if (y == chunksInView.vertical_end - debug - 1 || x == chunksInView.horizontal_start + debug ||
                    x == chunksInView.horizontal_end - debug - 1) {
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
        return skylight;
    }


    @Override
    public void dispose() {
        batch.dispose();
        chunkRenderer.dispose();
        rayHandler.dispose();
    }

    @Override
    public void resize(int width, int height) {
        rayHandler.resizeFBO(width / 4, height / 4);
        Vector3 old = camera.position.cpy();
        camera.setToOrtho(false, width, height);
        camera.position.set(old);
        update();
    }
}
