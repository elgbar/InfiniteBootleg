package no.elg.infiniteBootleg.world.ticker;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.LongMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import javax.annotation.concurrent.GuardedBy;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.api.Ticking;
import no.elg.infiniteBootleg.util.Ticker;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.time.WorldTime;
import no.elg.infiniteBootleg.world.world.ClientWorld;
import no.elg.infiniteBootleg.world.world.ServerWorld;
import no.elg.infiniteBootleg.world.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * There are multiple tickers for a single world internally, but the outside world should only see a
 * single, main, ticker. When pausing, stopping, resuming the main ticker all slave tickers should
 * do the same.
 *
 * <p>Multiple tickers are needed due to some ticks will happen less frequently.
 */
public class WorldTicker extends Ticker implements Disposable {

  public static final String WORLD_TICKER_TAG_PREFIX = "World-";
  @Nullable public final ServerRendererTicker serverRendererTicker;
  @NotNull public final WorldBox2DTicker box2DTicker;

  public WorldTicker(@NotNull World world, boolean tick) {
    super(
        new WorldTickee(world),
        WORLD_TICKER_TAG_PREFIX + world.getName(),
        tick,
        Settings.tps,
        Ticker.DEFAULT_NAG_DELAY);
    if (world instanceof ClientWorld) {
      serverRendererTicker = null;
    } else if (world instanceof ServerWorld serverWorld) {
      serverRendererTicker = new ServerRendererTicker(serverWorld, tick);
    } else {
      serverRendererTicker = null;
    }
    box2DTicker = new WorldBox2DTicker(world, tick);
  }

  private static class WorldTickee implements Ticking {

    @GuardedBy("world.chunksLock")
    @NotNull
    private final LongMap.Entries<@Nullable Chunk> chunkIterator;

    @NotNull private final World world;

    private WorldTickee(@NotNull World world) {
      this.world = world;
      chunkIterator = new LongMap.Entries<>(world.getChunks());
    }

    private final Array<ForkJoinTask<?>> forks = new Array<>(false, 48);

    @Override
    public synchronized void tick() {
      WorldRender wr = world.getRender();
      long chunkUnloadTime = world.getWorldTicker().getTps() * 5;

      // tick all chunks and blocks in chunks
      long tick = world.getWorldTicker().getTickId();
      ForkJoinPool pool = ForkJoinPool.commonPool();
      world.chunksLock.writeLock().lock();
      try {
        chunkIterator.reset();
        while (chunkIterator.hasNext()) {
          Chunk chunk = chunkIterator.next().value;

          // clean up dead chunks
          if (chunk == null) {
            Main.logger().warn("Found null chunk when ticking world");
            chunkIterator.remove();
            continue;
          } else if (chunk.isDisposed()) {
            Main.logger()
                .warn(
                    "Found disposed chunk ("
                        + chunk.getChunkX()
                        + ","
                        + chunk.getChunkY()
                        + ") when ticking world");
            chunkIterator.remove();
            continue;
          }

          if (chunk.isAllowedToUnload()
              && wr.isOutOfView(chunk)
              && tick - chunk.getLastViewedTick() > chunkUnloadTime) {

            chunkIterator.remove();
            world.unloadChunk(chunk);
            continue;
          }
          ForkJoinTask<?> task = pool.submit(chunk::tick);
          forks.add(task);
          task.fork();
        }
      } finally {
        world.chunksLock.writeLock().unlock();
      }
      //      for (Entity entity : world.getEntities()) {
      //        if (entity.isDisposed()) {
      //          String message =
      //              "Invalid entity in world entities ("
      //                  + entity.simpleName()
      //                  + ": "
      //                  + entity.hudDebug()
      //                  + ")";
      //          Main.logger().debug("WORLD", message);
      //          world.removeEntity(entity);
      //          continue;
      //        }
      //        ForkJoinTask<?> task = pool.submit(entity::tick);
      //        forks.add(task);
      //        task.fork();
      //      }
      for (ForkJoinTask<?> task : forks) {
        try {
          task.join();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      forks.clear();
    }

    @Override
    public void tickRare() {
      //      if(Main.isServer()){
      //        Main.logger().debug("PACKET INFO", "Server received " +
      // ServerBoundHandler.packetsReceived+" packets");
      //        ServerBoundHandler.packetsReceived = 0;
      //      }
      for (Chunk chunk : world.getLoadedChunks()) {
        chunk.tickRare();
      }
      //      for (Entity entity : world.getEntities()) {
      //        if (entity.isDisposed()) {
      //          continue;
      //        }
      //        entity.tickRare();
      //      }

      WorldTime time = world.getWorldTime();
      time.setTime(
          time.getTime()
              + world.getWorldTicker().getSecondsDelayBetweenTicks() * time.getTimeScale());
    }
  }

  @Override
  public void start() {
    super.start();
    box2DTicker.getTicker().start();

    while (getTickId() <= 0) {
      Thread.onSpinWait();
    }
    while (box2DTicker.getTicker().getTickId() <= 0) {
      Thread.onSpinWait();
    }

    if (serverRendererTicker != null) {
      serverRendererTicker.getTicker().start();
      while (serverRendererTicker.getTicker().getTickId() <= 0) {
        Thread.onSpinWait();
      }
    }
  }

  /** Stop this ticker, the tickers thread will not be called anymore */
  @Override
  public void stop() {
    super.stop();
    if (serverRendererTicker != null) {
      serverRendererTicker.getTicker().stop();
    }
    box2DTicker.getTicker().stop();
  }

  /**
   * Temporarily stops this ticker, can be resumed with {@link #resume()}
   *
   * @see #isPaused()
   * @see #resume()
   */
  @Override
  public void pause() {
    super.pause();
    if (serverRendererTicker != null) {
      serverRendererTicker.getTicker().pause();
    }
    box2DTicker.getTicker().pause();
  }

  /**
   * Resume the ticking thread if it {@link #isPaused()}
   *
   * @see #isPaused()
   * @see #pause()
   */
  @Override
  public void resume() {
    super.resume();
    if (serverRendererTicker != null) {
      serverRendererTicker.getTicker().resume();
    }
    box2DTicker.getTicker().resume();
  }

  @Override
  public void dispose() {
    stop();
  }
}
