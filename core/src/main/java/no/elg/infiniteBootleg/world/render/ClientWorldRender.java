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
import no.elg.infiniteBootleg.world.ClientWorld;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import no.elg.infiniteBootleg.world.time.WorldTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public class ClientWorldRender implements WorldRender {

  @NotNull public final ClientWorld world;
  @NotNull private final Rectangle viewBound;
  @NotNull private final ClientChunksInView chunksInView;
  @NotNull private final Matrix4 m4 = new Matrix4();
  @NotNull OrderedMap<Chunk, TextureRegion> draw;
  @NotNull private final PublicRayHandler rayHandler;
  @NotNull private final EntityRenderer entityRenderer;
  @NotNull private final SpriteBatch batch;
  @NotNull private final OrthographicCamera camera;
  @NotNull private final ChunkRenderer chunkRenderer;
  @NotNull private final Box2DDebugRenderer box2DDebugRenderer;
  @NotNull private final DebugChunkRenderer chunkDebugRenderer;
  @Nullable private DirectionalLight sun;
  @Nullable private DirectionalLight ambientLight;
  private float lastZoom;

  static {
    RayHandler.setGammaCorrection(true);
    RayHandler.useDiffuseLight(true);
  }

  public ClientWorldRender(@NotNull ClientWorld world) {
    viewBound = new Rectangle();
    chunksInView = new ClientChunksInView();
    this.world = world;
    draw = new OrderedMap<>();
    draw.orderedKeys().ordered = false; // improve remove

    chunkRenderer = new ChunkRenderer(this);
    entityRenderer = new EntityRenderer(this);

    camera = new OrthographicCamera();
    camera.setToOrtho(false);

    camera.zoom = 1f;
    camera.position.x = 0;
    camera.position.y = 0;

    batch = new SpriteBatch();
    batch.setProjectionMatrix(camera.combined);

    chunkDebugRenderer = new DebugChunkRenderer(this);
    box2DDebugRenderer = new Box2DDebugRenderer();

    rayHandler = new PublicRayHandler(world.getWorldBody().getBox2dWorld());
    rayHandler.setBlurNum(1);
    rayHandler.setAmbientLight(AMBIENT_LIGHT, AMBIENT_LIGHT, AMBIENT_LIGHT, 1f);
    rayHandler.setCulling(true);
    Main.inst().getScheduler().executeSync(this::resetSkylight);
  }

  private DirectionalLight createDirLight() {
    var light =
        new DirectionalLight(
            rayHandler, blocksHorizontally() * RAYS_PER_BLOCK, Color.WHITE, WorldTime.MIDDAY_TIME);
    light.setStaticLight(true);
    light.setContactFilter(World.LIGHT_FILTER);
    light.setSoftnessLength(World.SKYLIGHT_SOFTNESS_LENGTH);
    return light;
  }

  private void resetSkylight() {
    synchronized (BOX2D_LOCK) {
      synchronized (LIGHT_LOCK) {
        if (sun != null) {
          sun.remove();
        }
        if (ambientLight != null) {
          ambientLight.remove();
        }
        sun = createDirLight();
        ambientLight = createDirLight();

        rayHandler.update();

        // Re-render at once otherwise a quick flickering might happen
        update();
        render();
      }
    }
  }

  @Override
  public void render() {
    chunkRenderer.render(20);

    draw.clear();
    draw.ensureCapacity(chunksInView.getChunksInView());

    final WorldBody worldBody = world.getWorldBody();
    float worldOffsetX = worldBody.getWorldOffsetX() * BLOCK_SIZE;
    float worldOffsetY = worldBody.getWorldOffsetY() * BLOCK_SIZE;
    int verticalStart = chunksInView.getVerticalStart();
    int verticalEnd = chunksInView.getVerticalEnd();
    for (int y = verticalStart; y < verticalEnd; y++) {
      int horizontalStart = chunksInView.getHorizontalStart();
      int horizontalEnd = chunksInView.getHorizontalEnd();
      for (int x = horizontalStart; x < horizontalEnd; x++) {
        Chunk chunk = world.getChunk(x, y);
        if (chunk == null) {
          continue;
        }
        chunk.view();

        // No need to update texture when out of view, but in loaded zone
        if (y == verticalEnd - 1
            || y == verticalStart
            || x == horizontalStart
            || x == horizontalEnd - 1) {
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

        // get texture here to update last viewed in chunk
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
      rayHandler.prepareRender();
      synchronized (BOX2D_LOCK) {
        synchronized (LIGHT_LOCK) {
          rayHandler.renderLightMap();
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
      viewBound.set(
          (camera.position.x - worldOffsetX) - w / 2,
          (camera.position.y - worldOffsetY) - h / 2,
          w,
          h);

      chunksInView.setHorizontalStart(
          MathUtils.floor(viewBound.x / CHUNK_TEXTURE_SIZE) - CHUNKS_IN_VIEW_HORIZONTAL_PHYSICS);
      chunksInView.setHorizontalEnd(
          MathUtils.floor((viewBound.x + viewBound.width + CHUNK_TEXTURE_SIZE) / CHUNK_TEXTURE_SIZE)
              + CHUNKS_IN_VIEW_HORIZONTAL_PHYSICS);

      chunksInView.setVerticalStart(
          MathUtils.floor(viewBound.y / CHUNK_TEXTURE_SIZE) - CHUNKS_IN_VIEW_PADDING_RENDER);
      chunksInView.setVerticalEnd(
          MathUtils.floor(
                  (viewBound.y + viewBound.height + CHUNK_TEXTURE_SIZE) / CHUNK_TEXTURE_SIZE)
              + CHUNKS_IN_VIEW_PADDING_RENDER);

      ChunksInView.Companion.forEach(
          chunksInView,
          world,
          chunk -> {
            if (!chunk.hasTextureRegion()) {
              chunk.dirty();
            }
            return null;
          });

      if (Math.abs(lastZoom - camera.zoom) > SKYLIGHT_ZOOM_THRESHOLD) {
        lastZoom = camera.zoom;
        resetSkylight();
      }
    }
  }

  public void reload() {
    synchronized (BOX2D_LOCK) {
      synchronized (LIGHT_LOCK) {
        rayHandler.removeAll();
        PointLightPool.clearAllPools();
        sun = null; // do not dispose skylight, it has already been disposed here
        ambientLight = null;

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

  @Override
  public void resize(int width, int height) {
    Vector3 old = camera.position.cpy();
    camera.setToOrtho(false, width, height);
    camera.position.set(old);
    update();
  }

  @Override
  public void dispose() {
    batch.dispose();
    chunkRenderer.dispose();
    rayHandler.dispose();
    box2DDebugRenderer.dispose();
    chunkDebugRenderer.dispose();
  }

  @Override
  public @NotNull ClientWorld getWorld() {
    return world;
  }

  /**
   * @return How many blocks there currently are horizontally on screen
   */
  public int blocksHorizontally() {
    return (int) Math.ceil(camera.viewportWidth * camera.zoom / BLOCK_SIZE) + 1;
  }

  @Override
  public boolean isOutOfView(@NotNull Chunk chunk) {
    return getChunksInView().isOutOfView(chunk.getChunkX(), chunk.getChunkY());
  }

  @NotNull
  public ClientChunksInView getChunksInView() {
    return chunksInView;
  }

  public @NotNull OrthographicCamera getCamera() {
    return camera;
  }

  public @NotNull ChunkRenderer getChunkRenderer() {
    return chunkRenderer;
  }

  public @NotNull SpriteBatch getBatch() {
    return batch;
  }

  public @NotNull PublicRayHandler getRayHandler() {
    return rayHandler;
  }

  public @Nullable DirectionalLight getSun() {
    return sun;
  }

  public DirectionalLight getAmbientLight() {
    return ambientLight;
  }
}
