package no.elg.infiniteBootleg.world.render;

import box2dLight.DirectionalLight;
import box2dLight.PublicRayHandler;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Renderer;
import no.elg.infiniteBootleg.Updatable;
import no.elg.infiniteBootleg.util.Resizable;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @author Elg */
public interface WorldRender extends Updatable, Renderer, Disposable, Resizable {

  float MIN_ZOOM = 0.25f;
  float MAX_ZOOM = 1.75f;
  float AMBIENT_LIGHT = 0.026f;
  int RAYS_PER_BLOCK = 200;
  /**
   * How many chunks around extra should be included.
   *
   * <p>One of the these chunks comes from the need that the player should not see that a chunks
   * loads in.
   */
  int CHUNKS_IN_VIEW_PADDING_RENDER = 1;
  /**
   * How many chunks to the sides extra should be included.
   *
   * <p>This is the for light during twilight to stop light bleeding into the sides of the screen
   * when moving.
   */
  int CHUNKS_IN_VIEW_HORIZONTAL_PHYSICS = CHUNKS_IN_VIEW_PADDING_RENDER + 1;
  /**
   * How much must the player zoom to trigger a skylight reset
   *
   * @see #resetSkylight()
   */
  float SKYLIGHT_ZOOM_THRESHOLD = 0.25f;
  /**
   * How many {@link Graphics#getFramesPerSecond()} should there be when rendering multiple chunks
   */
  int FPS_FAST_CHUNK_RENDER_THRESHOLD = 10;

  Object LIGHT_LOCK = new Object();
  Object BOX2D_LOCK = new Object();

  void resetSkylight();

  /** @return How many blocks there currently are horizontally on screen */
  int blocksHorizontally();

  /**
   * @param chunk The chunk to check
   * @return {@code true} if the given chunk is outside the view of the camera
   */
  boolean isOutOfView(@NotNull Chunk chunk);

  @NotNull
  ChunksInView getChunksInView();

  @Nullable
  OrthographicCamera getCamera();

  @Nullable
  ChunkRenderer getChunkRenderer();

  EntityRenderer getEntityRenderer();

  @Nullable
  SpriteBatch getBatch();

  @Nullable
  PublicRayHandler getRayHandler();

  @Nullable
  DirectionalLight getSkylight();

  @NotNull
  World getWorld();

  void reload();
}
