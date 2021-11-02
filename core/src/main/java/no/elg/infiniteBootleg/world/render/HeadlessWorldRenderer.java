package no.elg.infiniteBootleg.world.render;

import box2dLight.DirectionalLight;
import box2dLight.PublicRayHandler;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @author Elg */
public record HeadlessWorldRenderer(World world) implements WorldRender {

  private static final ChunksInView HEADLESS_CHUNK_VIEWED = new ChunksInView();

  public HeadlessWorldRenderer(@NotNull World world) {
    this.world = world;
  }

  @Override
  public void render() {
    for (Chunk chunk : world.getLoadedChunks()) {
      if (chunk.isDirty()) {
        chunk.getChunkBody().update(true);
      }
    }
  }

  @Override
  public void dispose() {}

  @Override
  public void resize(int width, int height) {}

  @Override
  public void update() {}

  @Override
  public void resetSkylight() {}

  @Override
  public int blocksHorizontally() {
    return 0;
  }

  @Override
  public boolean isOutOfView(@NotNull Chunk chunk) {
    return false;
  }

  @Override
  public @NotNull ChunksInView getChunksInView() {
    return HEADLESS_CHUNK_VIEWED;
  }

  @Override
  public @Nullable OrthographicCamera getCamera() {
    return null;
  }

  @Override
  public @Nullable ChunkRenderer getChunkRenderer() {
    return null;
  }

  @Override
  public EntityRenderer getEntityRenderer() {
    return null;
  }

  @Override
  public @Nullable SpriteBatch getBatch() {
    return null;
  }

  @Override
  public @Nullable PublicRayHandler getRayHandler() {
    return null;
  }

  @Override
  public @Nullable DirectionalLight getSkylight() {
    return null;
  }

  @Override
  public @NotNull World getWorld() {
    return world;
  }

  @Override
  public void reload() {}
}
