package no.elg.infiniteBootleg.world.render;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.ServerWorld;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public class HeadlessWorldRenderer implements WorldRender {

  @NotNull
  private final ObjectMap<@NotNull UUID, ServerClientChunksInView> viewingChunks =
      new OrderedMap<>();

  @NotNull private final ServerWorld world;

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final Lock readLock = lock.readLock();
  private final Lock writeLock = lock.writeLock();

  public HeadlessWorldRenderer(@NotNull ServerWorld world) {
    this.world = world;
  }

  @Override
  public synchronized void render() {
    // Note to self: do not call chunkBody#update while under the chunksLock.readLock() or
    // chunksLock.writeLock()
    for (Chunk chunk : world.getLoadedChunks()) {
      if (chunk.isValid() && chunk.isDirty()) {
        chunk.getChunkBody().update();
      }
    }
  }

  @Override
  public void dispose() {}

  @Override
  public void resize(int width, int height) {}

  @Override
  public void update() {
    readLock.lock();
    try {
      for (Player player : world.getPlayers()) {
        @Nullable var chunksInView = viewingChunks.get(player.getUuid());
        if (chunksInView != null) {
          Vector2 position = player.getPosition();
          chunksInView.setCenter(
              CoordUtil.worldToChunk(position.x), CoordUtil.worldToChunk(position.y));
        }
      }
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public boolean isOutOfView(@NotNull Chunk chunk) {
    readLock.lock();
    try {
      for (ServerClientChunksInView inView : viewingChunks.values()) {
        if (inView.isInView(chunk.getChunkX(), chunk.getChunkY())) {
          return false;
        }
      }
      return true;
    } finally {
      readLock.unlock();
    }
  }

  public void addClient(@NotNull UUID uuid, @NotNull ServerClientChunksInView civ) {
    writeLock.lock();
    try {
      viewingChunks.put(uuid, civ);
    } finally {
      writeLock.unlock();
    }
  }

  public void removeClient(@NotNull UUID uuid) {
    writeLock.lock();
    try {
      viewingChunks.remove(uuid);
    } finally {
      writeLock.unlock();
    }
  }

  @Nullable
  public ServerClientChunksInView getClient(@NotNull UUID uuid) {
    readLock.lock();
    try {
      return viewingChunks.get(uuid);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public @NotNull ServerWorld getWorld() {
    return world;
  }
}
