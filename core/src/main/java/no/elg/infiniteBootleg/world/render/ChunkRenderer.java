package no.elg.infiniteBootleg.world.render;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_TEXTURE_SIZE;
import static no.elg.infiniteBootleg.world.Material.AIR;
import static no.elg.infiniteBootleg.world.render.WorldRender.FPS_FAST_CHUNK_RENDER_THRESHOLD;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.LongMap;
import java.util.LinkedList;
import java.util.List;
import no.elg.infiniteBootleg.Renderer;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
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

  public static final int LIGHT_PER_BLOCK = 2;
  public static final double LIGHT_SOURCE_LOOK_BLOCKS = 4.0;

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
      int topBlockHeight = chunkColumn.topBlockHeight(x);
      for (int y = 0; y < CHUNK_SIZE; y++) {
        Block block = blocks[x][y];

        if (block != null && block.getMaterial().isEntity()) {
          continue;
        } else if ((block == null || block.getMaterial() == AIR)) {
          if (topBlockHeight > CoordUtil.chunkToWorld(chunk.getChunkY(), y)) {
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

        // find light sources around this block

        //        //Distance to neasest light source
        //        int above = 0, below = 0, left = 0, right = 0;

        // outermap: compact loc to sub light cell
        // inner array: distance to all sources in blocks^2
        LongMap<Double> lightMap = new LongMap<>(LIGHT_PER_BLOCK * LIGHT_PER_BLOCK);
        int lightSources = 0;

        // skylight
        int worldX = block.getWorldX();
        int worldY = block.getWorldY();

        Array<@NotNull Block> blocksAABB =
            chunk
                .getWorld()
                .getBlocksAABB(
                    worldX + 0.5f,
                    worldY + 0.5f,
                    (float) LIGHT_SOURCE_LOOK_BLOCKS,
                    (float) LIGHT_SOURCE_LOOK_BLOCKS,
                    false);
        for (Block neighbor : blocksAABB) {
          //          if (neighbor.getMaterial() == AIR || ) {
          //            System.out.println("test " + (neighbor.getWorldY() > topBlockHeight &&
          // neighbor.getMaterial() == AIR) + " first " + (neighbor.getWorldY() > topBlockHeight));
          //          }
          if (neighbor.getMaterial().isLuminescent()
              || (neighbor.getWorldY() >= topBlockHeight && neighbor.getMaterial() == AIR)) {
            lightSources++;
            for (int lx = 0; lx < LIGHT_PER_BLOCK; lx++) {
              for (int ly = 0; ly < LIGHT_PER_BLOCK; ly++) {
                // Calculate distance for each light cell
                var dist =
                    (Location.distCubed(
                            worldX + ((float) lx / LIGHT_PER_BLOCK),
                            worldY + ((float) ly / LIGHT_PER_BLOCK),
                            neighbor.getWorldX() + 0.5,
                            neighbor.getWorldY() + 0.5))
                        / (LIGHT_SOURCE_LOOK_BLOCKS * LIGHT_SOURCE_LOOK_BLOCKS);
                //                System.out.println("dist " + dist);
                long key = CoordUtil.compactLoc(lx, ly);
                var old = lightMap.get(key, Double.MAX_VALUE);
                if (old > dist) {
                  lightMap.put(key, dist);
                }
              }
            }
          }
        }

        assert texture != null;

        if (lightSources > 0) {

          int tileWidth = texture.getRegionWidth() / LIGHT_PER_BLOCK;
          int tileHeight = texture.getRegionHeight() / LIGHT_PER_BLOCK;
          TextureRegion[][] split = texture.split(tileWidth, tileHeight);

          for (int ry = 0, splitLength = split.length; ry < splitLength; ry++) {
            TextureRegion[] regions = split[LIGHT_PER_BLOCK - ry - 1];
            for (int rx = 0, regionsLength = regions.length; rx < regionsLength; rx++) {
              TextureRegion region = regions[rx];

              double rawIntensity = lightMap.get(CoordUtil.compactLoc(rx, ry), 0.0);
              float normalizedIntensity;
              if (rawIntensity == 0.0) {
                normalizedIntensity = 0f;
              } else if (rawIntensity > 0) {
                normalizedIntensity = 1 - (float) (rawIntensity);
              } else {
                normalizedIntensity = 1 + (float) (rawIntensity);
              }

              if (rawIntensity != 0.0 && lightSources > 1)
                System.out.println(
                    "rawIntensity "
                        + rawIntensity
                        + ", normalizedIntensity "
                        + normalizedIntensity
                        + " lightSources "
                        + lightSources);

              //            float color = ((ry / (float) LIGHT_PER_BLOCK) + (rx / (float)
              // LIGHT_PER_BLOCK) + 0.25f) / 2.25f;

              batch.setColor(normalizedIntensity, normalizedIntensity, normalizedIntensity, 1);
              batch.draw(
                  region,
                  dx + rx * tileWidth,
                  dy + ry * tileHeight,
                  BLOCK_SIZE / (float) LIGHT_PER_BLOCK,
                  BLOCK_SIZE / (float) LIGHT_PER_BLOCK);
            }
          }
        } else {
          batch.setColor(Color.BLACK);
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

  @Override
  public void dispose() {
    batch.dispose();
    synchronized (QUEUE_LOCK) {
      renderQueue.clear();
    }
  }
}
