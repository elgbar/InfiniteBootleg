package no.elg.infiniteBootleg.world.render;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_TEXTURE_SIZE;
import static no.elg.infiniteBootleg.world.Material.AIR;
import static no.elg.infiniteBootleg.world.render.WorldRender.FPS_FAST_CHUNK_RENDER_THRESHOLD;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import java.util.LinkedList;
import java.util.List;
import no.elg.infiniteBootleg.Renderer;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.blocks.TntBlock;
import org.apache.commons.collections4.list.SetUniqueList;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class ChunkRenderer implements Renderer, Disposable {

  private final SpriteBatch batch;
  private final SetUniqueList<Chunk> renderQueue;
  private final WorldRender worldRender;

  // current rendering chunk
  private Chunk curr;
  private static final Object QUEUE_LOCK = new Object();

  public ChunkRenderer(@NotNull WorldRender worldRender) {
    this.worldRender = worldRender;
    batch = new SpriteBatch();
    // use linked list for fast adding to end and beginning
    List<Chunk> chunkList = new LinkedList<>();
    renderQueue = SetUniqueList.setUniqueList(chunkList);
    batch.setProjectionMatrix(
        new Matrix4().setToOrtho2D(0, 0, CHUNK_TEXTURE_SIZE, Chunk.CHUNK_TEXTURE_SIZE));
    batch.disableBlending();
  }

  public void queueRendering(@NotNull Chunk chunk, boolean prioritize) {
    synchronized (QUEUE_LOCK) {
      // do not queue the chunk we're currently rendering
      if (chunk != curr && !renderQueue.contains(chunk)) {
        if (prioritize) {
          renderQueue.add(0, chunk);
        } else {
          renderQueue.add(chunk);
        }
      }
    }
  }

  public void render(int times) {
    render();
    if (Gdx.graphics.getFramesPerSecond() > FPS_FAST_CHUNK_RENDER_THRESHOLD) {
      // only render more chunks when the computer isn't struggling with the rendering
      for (int i = 0; i < times; i++) {
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
    synchronized (QUEUE_LOCK) {
      do {
        if (renderQueue.isEmpty()) {
          return;
        } // nothing to render
        chunk = renderQueue.remove(0);
      } while ((chunk.isAllAir()
              && !chunk
                  .getWorld()
                  .getChunkColumn(chunk.getChunkX())
                  .isChunkBelowTopBlock(chunk.getChunkY()))
          || !chunk.isLoaded()
          || worldRender.isOutOfView(chunk));
      curr = chunk;
    }

    FrameBuffer fbo = chunk.getFbo();
    var chunkColumn = chunk.getWorld().getChunkColumn(chunk.getChunkX());

    // this is the main render function
    Block[][] blocks = chunk.getBlocks();
    fbo.begin();
    batch.begin();
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

    for (int x = 0; x < CHUNK_SIZE; x++) {
      for (int y = 0; y < CHUNK_SIZE; y++) {
        Block block = blocks[x][y];

        if (block != null && block.getMaterial().isEntity()) {
          continue;
        } else if ((block == null || block.getMaterial() == AIR)) {
          if (chunkColumn.topBlockHeight(x) > CoordUtil.chunkToWorld(chunk.getChunkY(), y)) {
            block = chunk.setBlock(x, y, AIR, false);
          } else {
            continue;
          }
        }
        TextureRegion texture;
        float a;
        if (block.getMaterial() == AIR) {
          texture = TntBlock.Companion.getWhiteTexture();
          a = 0.5f;
        } else {
          texture = block.getTexture();
          a = 1f;
        }
        int dx = block.getLocalX() * BLOCK_SIZE;
        int dy = block.getLocalY() * BLOCK_SIZE;

        assert texture != null;
        int splits = 4;
        int tileWidth = texture.getRegionWidth() / splits;
        int tileHeight = texture.getRegionHeight() / splits;

        TextureRegion[][] split = texture.split(tileWidth, tileHeight);
        for (int rx = 0, splitLength = split.length; rx < splitLength; rx++) {

          TextureRegion[] regions = split[rx];
          for (int ry = 0, regionsLength = regions.length; ry < regionsLength; ry++) {
            TextureRegion region = regions[ry];
            float color = ((rx / (float) splits) + (ry / (float) splits)) / 2f;
            batch.setColor(color, color, color, a);
            batch.draw(
                region,
                dx + rx * tileWidth,
                dy + ry * tileHeight,
                BLOCK_SIZE / (float) splits,
                BLOCK_SIZE / (float) splits);
          }
        }
      }
    }

    batch.end();
    fbo.end();

    synchronized (QUEUE_LOCK) {
      curr = null;
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
