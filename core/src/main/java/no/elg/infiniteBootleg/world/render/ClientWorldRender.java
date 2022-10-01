package no.elg.infiniteBootleg.world.render;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_TEXTURE_SIZE;
import static no.elg.infiniteBootleg.world.ChunkColumn.Companion.FeatureFlag.TOP_MOST_FLAG;
import static no.elg.infiniteBootleg.world.GlobalLockKt.BOX2D_LOCK;

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
import com.badlogic.gdx.utils.OrderedSet;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.ClientWorld;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class ClientWorldRender implements WorldRender {

  @NotNull public final ClientWorld world;
  @NotNull private final Rectangle viewBound;
  @NotNull private final ClientChunksInView chunksInView;
  @NotNull private final Matrix4 m4 = new Matrix4();
  @NotNull OrderedMap<Chunk, TextureRegion> draw;
  @NotNull private final EntityRenderer entityRenderer;
  @NotNull private final SpriteBatch batch;
  @NotNull private final OrthographicCamera camera;
  @NotNull private final ChunkRenderer chunkRenderer;
  @NotNull private final Box2DDebugRenderer box2DDebugRenderer;
  @NotNull private final DebugChunkRenderer chunkDebugRenderer;
  private float lastZoom;

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
    box2DDebugRenderer = new Box2DDebugRenderer(false, false, true, true, true, true);
  }

  @Override
  public void render() {
    batch.setProjectionMatrix(camera.combined);
    chunkRenderer.render(20);

    draw.clear();
    draw.ensureCapacity(chunksInView.getSize());

    WorldBody worldBody = world.getWorldBody();
    float worldOffsetX = worldBody.getWorldOffsetX() * BLOCK_SIZE;
    float worldOffsetY = worldBody.getWorldOffsetY() * BLOCK_SIZE;
    int verticalStart = chunksInView.getVerticalStart();
    int verticalEnd = chunksInView.getVerticalEnd();
    for (int chunkY = verticalStart; chunkY < verticalEnd; chunkY++) {
      int horizontalStart = chunksInView.getHorizontalStart();
      int horizontalEnd = chunksInView.getHorizontalEnd();
      for (int chunkX = horizontalStart; chunkX < horizontalEnd; chunkX++) {
        Chunk chunk = world.getChunk(chunkX, chunkY, false);
        if (chunk == null) {
          int loadingChunkX = chunkX;
          int loadingChunkY = chunkY;
          Main.inst()
              .getScheduler()
              .executeAsync(() -> world.loadChunk(loadingChunkX, loadingChunkY));
          continue;
        }
        chunk.view();

        // No need to update texture when out of view, but in loaded zone
        if (chunkY == verticalEnd - 1
            || chunkY == verticalStart
            || chunkX == horizontalStart
            || chunkX == horizontalEnd - 1) {
          continue;
        }

        //noinspection LibGDXFlushInsideLoop
        if (chunk.isAllAir()
            && chunk.getChunkColumn().isChunkAboveTopBlock(chunk.getChunkY(), TOP_MOST_FLAG)) {
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
    if (Settings.renderChunkBounds && Settings.debug) {
      chunkDebugRenderer.render();
    }
    if (Settings.renderBox2dDebug && Settings.debug) {
      synchronized (BOX2D_LOCK) {
        box2DDebugRenderer.render(worldBody.box2dWorld, m4);
      }
    }
  }

  @Override
  public void update() {
    camera.update();
    m4.set(camera.combined).scl(Block.BLOCK_SIZE);

    float width = camera.viewportWidth * camera.zoom;
    float height = camera.viewportHeight * camera.zoom;

    if (!getWorld().getWorldTicker().isPaused()) {

      WorldBody worldBody = world.getWorldBody();
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

      if (Math.abs(lastZoom - camera.zoom) > SKYLIGHT_ZOOM_THRESHOLD) {
        lastZoom = camera.zoom;
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
    return chunksInView.isOutOfView(chunk.getChunkX(), chunk.getChunkY());
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

  @NotNull
  @Override
  public OrderedSet<Location> getChunkLocationsInView() {
    return ChunksInView.Companion.toSet(chunksInView);
  }
}
