package no.elg.infiniteBootleg.world.render;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_TEXTURE_SIZE;

import box2dLight.DirectionalLight;
import box2dLight.PublicRayHandler;
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
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.util.PointLightPool;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import no.elg.infiniteBootleg.world.time.WorldTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public class ClientWorldRender implements WorldRender {

    @NotNull
    public final World world;
    @NotNull
    private final Rectangle viewBound;
    @NotNull
    private final ChunksInView chunksInView;
    @NotNull
    private final Matrix4 m4 = new Matrix4();
    @NotNull OrderedMap<Chunk, TextureRegion> draw;
    @Nullable
    private PublicRayHandler rayHandler;
    private EntityRenderer entityRenderer;
    @Nullable
    private SpriteBatch batch;
    @Nullable
    private OrthographicCamera camera;
    @Nullable
    private ChunkRenderer chunkRenderer;
    @Nullable
    private Box2DDebugRenderer box2DDebugRenderer;
    @Nullable
    private DebugChunkRenderer chunkDebugRenderer;
    @Nullable
    private DirectionalLight skylight;
    private float lastZoom;

    public ClientWorldRender(@NotNull World world) {
        viewBound = new Rectangle();
        chunksInView = new ChunksInView();
        this.world = world;
        draw = new OrderedMap<>();
        draw.orderedKeys().ordered = false; //improve remove

        if (Settings.client) {
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
            rayHandler = new PublicRayHandler(world.getWorldBody().getBox2dWorld(), 200, 140);
            rayHandler.setBlurNum(2);
            rayHandler.setAmbientLight(AMBIENT_LIGHT, AMBIENT_LIGHT, AMBIENT_LIGHT, 0f);
            Main.inst().getScheduler().executeSync(this::resetSkylight);
        }
    }

    @Override
    public void resetSkylight() {

        synchronized (BOX2D_LOCK) {
            synchronized (LIGHT_LOCK) {
                if (skylight != null) {
                    skylight.remove();
                }
                skylight = new DirectionalLight(rayHandler, blocksHorizontally() * RAYS_PER_BLOCK, Color.WHITE, WorldTime.SUNRISE_TIME);
                skylight.setStaticLight(true);
                skylight.setContactFilter(World.LIGHT_FILTER);
                skylight.setSoftnessLength(World.SKYLIGHT_SOFTNESS_LENGTH);


                rayHandler.setAmbientLight(0, 0, 0, 1);
                rayHandler.updateAndRender();

                //Re-render at once otherwise a quick flickering might happen
                update();
                render();
            }
        }
    }

    @Override
    public int blocksHorizontally() {
        return (int) Math.ceil(camera.viewportWidth * camera.zoom / BLOCK_SIZE) + 1;
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
        draw.ensureCapacity(chunksInView.getHorizontalLength() * chunksInView.getVerticalLength());


        final WorldBody worldBody = world.getWorldBody();
        float worldOffsetX = worldBody.getWorldOffsetX() * BLOCK_SIZE;
        float worldOffsetY = worldBody.getWorldOffsetY() * BLOCK_SIZE;
        for (int y = chunksInView.verticalStart; y < chunksInView.verticalEnd; y++) {
            for (int x = chunksInView.horizontalStart; x < chunksInView.horizontalEnd; x++) {
                Chunk chunk = world.getChunk(x, y);
                if (chunk == null) {
                    continue;
                }
                chunk.view();

                //No need to update texture when out of view, but in loaded zone
                if (y == chunksInView.verticalEnd - 1 || y == chunksInView.verticalStart || x == chunksInView.horizontalStart ||
                    x == chunksInView.horizontalEnd - 1) {
                    continue;
                }

                if (chunk.isDirty()) {
                    //noinspection LibGDXFlushInsideLoop
                    chunk.updateTextureIfDirty();
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
        for (ObjectMap.Entry<Chunk, TextureRegion> entry : draw.entries()) {
            var dx = entry.key.getChunkX() * CHUNK_TEXTURE_SIZE + worldOffsetX;
            var dy = entry.key.getChunkY() * CHUNK_TEXTURE_SIZE + worldOffsetY;
            batch.draw(entry.value, dx, dy, CHUNK_TEXTURE_SIZE, CHUNK_TEXTURE_SIZE);
        }
        entityRenderer.render();
        batch.end();
        if (Settings.renderLight) {

            synchronized (BOX2D_LOCK) {
                synchronized (LIGHT_LOCK) {
                    rayHandler.render();
                }
            }
        }
        if (Settings.renderBox2dDebug && Settings.debug) {
            synchronized (BOX2D_LOCK) {
                box2DDebugRenderer.render(world.getWorldBody().getBox2dWorld(), m4);
            }
            chunkDebugRenderer.render();
        }
    }

    @Override
    public boolean isOutOfView(@NotNull Chunk chunk) {
        return chunk.getChunkX() < chunksInView.horizontalStart || chunk.getChunkX() >= chunksInView.horizontalEnd ||
               chunk.getChunkY() < chunksInView.verticalStart || chunk.getChunkY() >= chunksInView.verticalEnd;
    }

    @Override
    public @NotNull ChunksInView getChunksInView() {
        return chunksInView;
    }

    @Override
    public @Nullable OrthographicCamera getCamera() {
        return camera;
    }

    @Override
    public @Nullable ChunkRenderer getChunkRenderer() {
        return chunkRenderer;
    }

    @Override
    public EntityRenderer getEntityRenderer() {
        return entityRenderer;
    }

    @Override
    public @Nullable SpriteBatch getBatch() {
        return batch;
    }


    @Override
    public @Nullable PublicRayHandler getRayHandler() {
        return rayHandler;
    }

    @Override
    public @Nullable DirectionalLight getSkylight() {
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
        Vector3 old = camera.position.cpy();
        camera.setToOrtho(false, width, height);
        camera.position.set(old);
        update();
    }

    @Override
    public void update() {
        camera.update();
        Gdx.app.postRunnable(() -> batch.setProjectionMatrix(camera.combined));
        m4.set(camera.combined).scl(Block.BLOCK_SIZE);

        final float width = camera.viewportWidth * camera.zoom;
        final float height = camera.viewportHeight * camera.zoom;

        if (Settings.renderLight) {
            rayHandler.setCombinedMatrix(m4, 0, 0, width, height);
        }

        if (!getWorld().getWorldTicker().isPaused()) {

            final WorldBody worldBody = world.getWorldBody();
            float worldOffsetX = worldBody.getWorldOffsetX() * BLOCK_SIZE;
            float worldOffsetY = worldBody.getWorldOffsetY() * BLOCK_SIZE;

            float w = width * Math.abs(camera.up.y) + height * Math.abs(camera.up.x);
            float h = height * Math.abs(camera.up.y) + width * Math.abs(camera.up.x);
            viewBound.set((camera.position.x - worldOffsetX) - w / 2, (camera.position.y - worldOffsetY) - h / 2, w, h);

            chunksInView.horizontalStart = //
                MathUtils.floor(viewBound.x / CHUNK_TEXTURE_SIZE) - CHUNKS_IN_VIEW_HORIZONTAL_PHYSICS;
            chunksInView.horizontalEnd = //
                MathUtils.floor((viewBound.x + viewBound.width + CHUNK_TEXTURE_SIZE) / CHUNK_TEXTURE_SIZE) + CHUNKS_IN_VIEW_HORIZONTAL_PHYSICS;

            chunksInView.verticalStart = MathUtils.floor(viewBound.y / CHUNK_TEXTURE_SIZE) - CHUNKS_IN_VIEW_PADDING_RENDER;
            chunksInView.verticalEnd = //
                MathUtils.floor((viewBound.y + viewBound.height + CHUNK_TEXTURE_SIZE) / CHUNK_TEXTURE_SIZE) + CHUNKS_IN_VIEW_PADDING_RENDER;

            if (Math.abs(lastZoom - camera.zoom) > SKYLIGHT_ZOOM_THRESHOLD) {
                lastZoom = camera.zoom;
                resetSkylight();
            }
        }
    }

    @Override
    public @NotNull World getWorld() {
        return world;
    }

    @Override
    public void reload() {
        synchronized (BOX2D_LOCK) {
            synchronized (LIGHT_LOCK) {
                rayHandler.removeAll();
                PointLightPool.clearAllPools();
                skylight = null; //do not dispose skylight, it has already been disposed here


                final int active = rayHandler.getEnabledLights().size;
                if (active != 0) {
                    Main.logger().error("LIGHT", "There are " + active + " active lights after reload");
                }
                final int disabled = rayHandler.getDisabledLights().size;
                if (disabled != 0) {
                    Main.logger().error("LIGHT", "There are " + disabled + " disabled lights after reload");
                }

                resetSkylight();

                rayHandler.update();
            }
        }
    }

}