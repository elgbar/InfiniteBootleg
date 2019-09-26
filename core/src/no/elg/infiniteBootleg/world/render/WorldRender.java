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
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.util.Resizable;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_TEXTURE_SIZE;

/**
 * @author Elg
 */
public class WorldRender implements Updatable, Renderer, Disposable, Resizable {

    public static final int VERT_START = 0;
    public static final int VERT_END = 1;
    public static final int HOR_START = 2;
    public static final int HOR_END = 3;

    public static final float MIN_ZOOM = 0.25f;

    public final World world;
    private RayHandler rayHandler;
    private EntityRenderer entityRenderer;
    private SpriteBatch batch;

    private OrthographicCamera camera;
    private final Rectangle viewBound;
    private final int[] chunksInView;
    private ChunkRenderer chunkRenderer;
    private Box2DDebugRenderer debugRenderer;

    private Matrix4 m4;
    private DirectionalLight skylight;

    public static boolean lights = true;
    public static boolean debugBox2d = false;

    private int skyDir;

    public final static Object LIGHT_LOCK = new Object();
    public final static Object BOX2D_LOCK = new Object();

    public WorldRender(@NotNull World world) {
        viewBound = new Rectangle();
        chunksInView = new int[4];
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

            debugRenderer = new Box2DDebugRenderer();


            RayHandler.setGammaCorrection(true);
            RayHandler.useDiffuseLight(true);
            rayHandler = new RayHandler(world.getBox2dWorld());
            rayHandler.setBlurNum(1);
            rayHandler.setAmbientLight(0.025f, 0.025f, 0.025f, 1);

            skyDir = World.STRAIGHT_DOWN_SKYLIGHT;

            //TODO maybe use the zoom level to get a nice number of rays? ie width*zoom*4 or something
            skylight = new DirectionalLight(rayHandler, 7500, Color.WHITE, skyDir);
            skylight.setContactFilter(World.LIGHT_FILTER);
            skylight.setStaticLight(true);
        }
    }

    @Override
    public void updateRare() {
        update();
    }

    @Override
    public void update() {
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        m4 = camera.combined.cpy().scl(Block.BLOCK_SIZE);

        if (!getWorld().getWorldTicker().isPaused()) {
            float width = camera.viewportWidth * camera.zoom;
            float height = camera.viewportHeight * camera.zoom;

            float w = width * Math.abs(camera.up.y) + height * Math.abs(camera.up.x);
            float h = height * Math.abs(camera.up.y) + width * Math.abs(camera.up.x);
            viewBound.set(camera.position.x - w / 2, camera.position.y - h / 2, w, h);

            chunksInView[HOR_START] = MathUtils.floor(viewBound.x / CHUNK_TEXTURE_SIZE) - 1;
            chunksInView[HOR_END] = MathUtils.floor(
                (viewBound.x + viewBound.width + CHUNK_TEXTURE_SIZE) / CHUNK_TEXTURE_SIZE) + 1;

            chunksInView[VERT_START] = MathUtils.floor(viewBound.y / CHUNK_TEXTURE_SIZE);
            //add one to make sure we are always in darkness underground
            chunksInView[VERT_END] = MathUtils.floor(
                (viewBound.y + viewBound.height + CHUNK_TEXTURE_SIZE) / CHUNK_TEXTURE_SIZE) + 1;
        }
        if (lights) {
            skylight.setStaticLight(true);
            rayHandler.setCombinedMatrix(m4, Main.inst().getMouseBlockX(), Main.inst().getMouseBlockY(),
                                         camera.viewportWidth * camera.zoom, camera.viewportHeight * camera.zoom);
        }
    }


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
        int colEnd = chunksInView[HOR_END];
        int colStart = chunksInView[HOR_START];
        int rowEnd = chunksInView[VERT_END];
        int rowStart = chunksInView[VERT_START];

        //set to 1 to debug what chunks are rendered
        int debug = 0;

        batch.begin();
        for (int y = rowStart + debug; y < rowEnd - debug; y++) {
            for (int x = colStart + debug; x < colEnd - debug; x++) {
                Chunk chunk = world.getChunk(x, y);
                if (chunk.isAllAir()) {
                    continue;
                }
                TextureRegion textureRegion =
                    chunk.getTextureRegion(); //get texture here to update last viewed in chunk
                if (textureRegion == null) {
                    chunkRenderer.queueRendering(chunk, false);
                    continue;
                }

                float dx = chunk.getChunkX() * CHUNK_TEXTURE_SIZE;
                float dy = chunk.getChunkY() * CHUNK_TEXTURE_SIZE;
                batch.draw(textureRegion, dx, dy, CHUNK_TEXTURE_SIZE, CHUNK_TEXTURE_SIZE);

            }
        }
        entityRenderer.render();
        batch.end();
        if (lights) {
            synchronized (LIGHT_LOCK) {
                rayHandler.render();
            }
        }
        if (debugBox2d && Main.debug) {
            synchronized (BOX2D_LOCK) {
                debugRenderer.render(world.getBox2dWorld(), m4);
            }
        }
    }

    /**
     * @param chunk
     *     The chunk to check
     *
     * @return {@code true} if the given chunk is outside the view of the camera
     */
    public boolean isOutOfView(@NotNull Chunk chunk) {
        return chunk.getChunkX() < chunksInView[HOR_START] || chunk.getChunkX() >= chunksInView[HOR_END] ||
               chunk.getChunkY() < chunksInView[VERT_START] || chunk.getChunkY() >= chunksInView[VERT_END];
    }

    public int[] getChunksInView() {
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
    }
}
