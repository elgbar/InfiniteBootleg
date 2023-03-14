package no.elg.infiniteBootleg.world.render;

import static no.elg.infiniteBootleg.ClientMain.CLEAR_COLOR_A;
import static no.elg.infiniteBootleg.ClientMain.CLEAR_COLOR_B;
import static no.elg.infiniteBootleg.ClientMain.CLEAR_COLOR_G;
import static no.elg.infiniteBootleg.ClientMain.CLEAR_COLOR_R;
import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_TEXTURE_SIZE;
import static no.elg.infiniteBootleg.world.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG;
import static no.elg.infiniteBootleg.world.ChunkColumn.Companion.FeatureFlag.TOP_MOST_FLAG;
import static no.elg.infiniteBootleg.world.Material.AIR;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import java.util.LinkedList;
import java.util.List;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.api.Renderer;
import no.elg.infiniteBootleg.util.CoordUtilKt;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import org.apache.commons.collections4.list.SetUniqueList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public class ChunkRenderer implements Renderer, Disposable {

  /** How many [Graphics.getFramesPerSecond] should there be when rendering multiple chunks */
  public static final int FPS_FAST_CHUNK_RENDER_THRESHOLD = 10;

  public static final int EXTRA_CHUNKS_TO_RENDER_EACH_FRAME = 4;
  public static final int LIGHT_RESOLUTION = 2;
  private final SpriteBatch batch;
  private final SetUniqueList<Chunk> renderQueue;
  private final WorldRender worldRender;

  // current rendering chunk
  private Chunk curr;
  private static final Object QUEUE_LOCK = new Object();
  @NotNull private static final TextureRegion CAVE_TEXTURE;
  @NotNull private static final TextureRegion SKY_TEXTURE;

  public static final float CAVE_CLEAR_COLOR_R = 0.408824f;
  public static final float CAVE_CLEAR_COLOR_G = 0.202941f;
  public static final float CAVE_CLEAR_COLOR_B = 0.055882f;

  private static final int CHUNK_NOT_IN_QUEUE_INDEX = -1;

  static {
    var skyPixmap = new Pixmap(BLOCK_SIZE, BLOCK_SIZE, Pixmap.Format.RGBA4444);
    skyPixmap.setColor(CLEAR_COLOR_R, CLEAR_COLOR_G, CLEAR_COLOR_B, CLEAR_COLOR_A);
    skyPixmap.fill();
    SKY_TEXTURE = new TextureRegion(new Texture(skyPixmap));
    skyPixmap.dispose();

    var cavePixmap = new Pixmap(BLOCK_SIZE, BLOCK_SIZE, Pixmap.Format.RGBA4444);
    cavePixmap.setColor(CAVE_CLEAR_COLOR_R, CAVE_CLEAR_COLOR_G, CAVE_CLEAR_COLOR_B, CLEAR_COLOR_A);
    cavePixmap.fill();
    CAVE_TEXTURE = new TextureRegion(new Texture(cavePixmap));
    cavePixmap.dispose();
  }

  public ChunkRenderer(@NotNull WorldRender worldRender) {
    this.worldRender = worldRender;
    batch = new SpriteBatch();
    // use linked list for fast adding to end and beginning
    List<Chunk> chunkList = new LinkedList<>();
    renderQueue = SetUniqueList.setUniqueList(chunkList);
    batch.setProjectionMatrix(
        new Matrix4().setToOrtho2D(0, 0, CHUNK_TEXTURE_SIZE, Chunk.CHUNK_TEXTURE_SIZE));
  }

  public void queueRendering(@NotNull Chunk chunk, boolean prioritize) {
    queueRendering(chunk, prioritize, false);
  }

  /**
   * Queue rendering of a chunk. If the chunk is already in the queue to be rendered and {@code
   * prioritize} is {@code true} then the chunk will be moved to the front of the queue
   *
   * @param chunk The chunk to render
   * @param prioritize If the chunk should be placed at the front of the queue
   * @param forceAdd If the chunk should be rendered even if it is already in queue or is currently
   *     being rendered
   */
  public void queueRendering(@NotNull Chunk chunk, boolean prioritize, boolean forceAdd) {
    Main.inst()
        .getScheduler()
        .executeAsync(
            () -> {
              synchronized (QUEUE_LOCK) {
                var chunkIndex = renderQueue.indexOf(chunk);

                // Place the chunk at the front of the queue
                if (prioritize && chunkIndex != CHUNK_NOT_IN_QUEUE_INDEX) {
                  renderQueue.remove(chunkIndex);
                  renderQueue.add(0, chunk);
                  return;
                }

                // do not queue the chunk we're currently rendering
                if ((forceAdd || chunk != curr) && chunkIndex == CHUNK_NOT_IN_QUEUE_INDEX) {
                  if (prioritize) {
                    renderQueue.add(0, chunk);
                  } else {
                    renderQueue.add(chunk);
                  }
                }
              }
            });
  }

  public void renderMultiple() {
    render();
    if (Gdx.graphics.getFramesPerSecond() > FPS_FAST_CHUNK_RENDER_THRESHOLD) {
      // only render more chunks when the computer isn't struggling with the rendering
      for (int i = 0; i < EXTRA_CHUNKS_TO_RENDER_EACH_FRAME; i++) {
        if (renderQueue.isEmpty()) {
          return;
        }
        render();
      }
    }
  }

  @Override
  public void render() {
    // get the first valid chunk to render
    Chunk chunk;
    boolean aboveGround;
    synchronized (QUEUE_LOCK) {
      do {
        if (renderQueue.isEmpty()) {
          return;
        } // nothing to render
        chunk = renderQueue.remove(0);
        aboveGround = chunk.getChunkColumn().isChunkAboveTopBlock(chunk.getChunkY(), TOP_MOST_FLAG);
      } while ((chunk.isAllAir() && aboveGround)
          || !chunk.isNotDisposed()
          || worldRender.isOutOfView(chunk));
      curr = chunk;
    }

    FrameBuffer fbo = chunk.getFbo();
    if (fbo == null) {
      return;
    }
    var chunkColumn = chunk.getChunkColumn();

    // this is the main render function
    Block[][] blocks = chunk.getBlocks();
    fbo.begin();
    batch.begin();
    Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

    if (aboveGround) {
      Gdx.gl.glClearColor(CLEAR_COLOR_R, CLEAR_COLOR_G, CLEAR_COLOR_B, CLEAR_COLOR_A);
    } else {
      Gdx.gl.glClearColor(
          CAVE_CLEAR_COLOR_R, CAVE_CLEAR_COLOR_G, CAVE_CLEAR_COLOR_B, CLEAR_COLOR_A);
    }

    for (int localX = 0; localX < CHUNK_SIZE; localX++) {
      int topBlockHeight = chunkColumn.topBlockHeight(localX, BLOCKS_LIGHT_FLAG);
      for (int localY = 0; localY < CHUNK_SIZE; localY++) {
        @Nullable Block block = blocks[localX][localY];
        Material material = block != null ? block.getMaterial() : AIR;

        if (material.isEntity()) {
          continue;
        }
        var blockLight = chunk.getBlockLight(localX, localY);
        TextureRegion texture;
        TextureRegion secondaryTexture;
        int worldY = CoordUtilKt.chunkToWorld(chunk.getChunkY(), localY);
        if (material == AIR) {
          texture = (topBlockHeight > worldY) ? CAVE_TEXTURE : SKY_TEXTURE;
          secondaryTexture = null;
        } else {
          texture = block.getTexture();
          assert texture != null;

          if (material.isTransparent()) {
            secondaryTexture = (topBlockHeight > worldY) ? CAVE_TEXTURE : SKY_TEXTURE;
          } else {
            secondaryTexture = null;
          }
        }
        int dx = localX * BLOCK_SIZE;
        int dy = localY * BLOCK_SIZE;

        batch.setColor(Color.WHITE);

        if (Settings.renderLight && blockLight.isLit() && !blockLight.isSkylight()) {
          if (secondaryTexture != null) {
            // If the block is emitting light there is no point in drawing it shaded
            if (material.isLuminescent()) {
              batch.draw(secondaryTexture, dx, dy, BLOCK_SIZE, BLOCK_SIZE);
            } else {
              drawShadedBlock(secondaryTexture, blockLight.getLightMap(), dx, dy);
            }
          }
          drawShadedBlock(texture, blockLight.getLightMap(), dx, dy);

        } else {
          if (Settings.renderLight && !blockLight.isSkylight()) {
            batch.setColor(Color.BLACK);
          }
          if (secondaryTexture != null) {
            batch.draw(secondaryTexture, dx, dy, BLOCK_SIZE, BLOCK_SIZE);
          }
          batch.draw(texture, dx, dy, BLOCK_SIZE, BLOCK_SIZE);
        }
      }
    }

    batch.end();
    fbo.end();

    synchronized (QUEUE_LOCK) {
      curr = null;
    }
  }

  private void drawShadedBlock(TextureRegion texture, float[][] lights, float dx, float dy) {

    int tileWidth = texture.getRegionWidth() / LIGHT_RESOLUTION;
    int tileHeight = texture.getRegionHeight() / LIGHT_RESOLUTION;
    TextureRegion[][] split = texture.split(tileWidth, tileHeight);

    for (int ry = 0, splitLength = split.length; ry < splitLength; ry++) {
      TextureRegion[] regions = split[LIGHT_RESOLUTION - ry - 1];
      for (int rx = 0, regionsLength = regions.length; rx < regionsLength; rx++) {
        TextureRegion region = regions[rx];

        var lightIntensity = lights[rx][ry];
        batch.setColor(lightIntensity, lightIntensity, lightIntensity, 1f);
        batch.draw(
            region,
            dx + rx * tileWidth,
            dy + ry * tileHeight,
            BLOCK_SIZE / (float) LIGHT_RESOLUTION,
            BLOCK_SIZE / (float) LIGHT_RESOLUTION);
      }
    }
  }

  @Override
  public void dispose() {
    batch.dispose();
    synchronized (QUEUE_LOCK) {
      renderQueue.clear();
    }
  }
}
