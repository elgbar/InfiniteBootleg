package no.elg.infiniteBootleg.world.render;

import com.badlogic.gdx.utils.ObjectMap;
import java.util.UUID;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.ServerWorld;
import org.jetbrains.annotations.NotNull;

/** @author Elg */
public class HeadlessWorldRenderer implements WorldRender {

  @NotNull public final ObjectMap<@NotNull UUID, ChunksInView> viewingChunks = new ObjectMap<>();
  @NotNull private final ServerWorld world;

  public HeadlessWorldRenderer(@NotNull ServerWorld world) {
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
  public boolean isOutOfView(@NotNull Chunk chunk) {
    // TODO
    return false;
  }

  public ObjectMap<UUID, ChunksInView> getViewingChunks() {
    return viewingChunks;
  }

  @Override
  public @NotNull ServerWorld getWorld() {
    return world;
  }
}
