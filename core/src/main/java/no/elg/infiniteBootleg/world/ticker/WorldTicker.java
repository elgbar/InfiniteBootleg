package no.elg.infiniteBootleg.world.ticker;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.Ticking;
import no.elg.infiniteBootleg.util.Ticker;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.ClientWorld;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.ServerWorld;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.time.WorldTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * There are multiple tickers for a single world internally, but the outside world should only see a
 * single, main, ticker. When pausing, stopping, resuming the main ticker all slave tickers should
 * do the same.
 *
 * <p>Multiple tickers are needed due to some ticks will happen less frequently.
 */
public class WorldTicker extends Ticker {

  @Nullable private final WorldLightTicker lightTicker;
  @Nullable private final ServerRendererTicker serverRendererTicker;
  @NotNull private final WorldBox2DTicker box2DTicker;

  public WorldTicker(@NotNull World world, boolean tick) {
    super(
        new WorldTickee(world),
        "World-" + world.getName(),
        tick,
        Settings.tps,
        Ticker.DEFAULT_NAG_DELAY);
    if (world instanceof ClientWorld clientWorld) {
      lightTicker = new WorldLightTicker(clientWorld, tick);
      serverRendererTicker = null;
    } else if (world instanceof ServerWorld serverWorld) {
      lightTicker = null;
      serverRendererTicker = new ServerRendererTicker(serverWorld, tick);
    } else {
      lightTicker = null;
      serverRendererTicker = null;
    }
    box2DTicker = new WorldBox2DTicker(world, tick);
  }

  private static class WorldTickee implements Ticking {

    @NotNull private final OrderedMap.OrderedMapEntries<Location, Chunk> chunkIterator;
    @NotNull private final World world;

    private WorldTickee(@NotNull World world) {
      this.world = world;
      chunkIterator = new OrderedMap.OrderedMapEntries<>(world.getChunks());
    }

    private final Array<ForkJoinTask<?>> forks = new Array<>(false, 48);

    @Override
    public synchronized void tick() {

      WorldRender wr = world.getRender();
      long chunkUnloadTime = world.getWorldTicker().getTPS() * 5;

      // tick all chunks and blocks in chunks
      long tick = world.getWorldTicker().getTickId();
      ForkJoinPool pool = ForkJoinPool.commonPool();

      world.chunksWriteLock.lock();
      try {
        chunkIterator.reset();
        while (true) {
          Chunk chunk;
          if (chunkIterator.hasNext()) {
            ObjectMap.Entry<Location, Chunk> entry = chunkIterator.next();
            chunk = entry.value;
          } else {
            break;
          }

          // clean up dead chunks
          if (chunk == null || !chunk.isLoaded()) {
            chunkIterator.remove();
            continue;
          }
          if (chunk.isAllowingUnloading()
              && wr.isOutOfView(chunk)
              && tick - chunk.getLastViewedTick() > chunkUnloadTime) {

            world.unloadChunk(chunk);
            chunkIterator.remove();
            continue;
          }
          ForkJoinTask<?> task = pool.submit(chunk::tick);
          forks.add(task);
          task.fork();
        }
      } finally {
        world.chunksWriteLock.unlock();
      }
      for (Entity entity : world.getEntities()) {
        if (entity.isInvalid()) {
          String message =
              "Invalid entity in world entities ("
                  + entity.simpleName()
                  + ": "
                  + entity.hudDebug()
                  + ")";
          Main.logger().debug("WORLD", message);
          world.removeEntity(entity);
          continue;
        }
        ForkJoinTask<?> task = pool.submit(entity::tick);
        forks.add(task);
        task.fork();
      }
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
      for (Entity entity : world.getEntities()) {
        if (entity.isInvalid()) {
          continue;
        }
        entity.tickRare();
      }

      WorldTime time = world.getWorldTime();
      time.setTime(
          time.getTime()
              + world.getWorldTicker().getSecondsDelayBetweenTicks() * time.getTimeScale());
    }
  }

  @Override
  public void start() {
    super.start();
    if (lightTicker != null) {
      lightTicker.getTicker().start();
    }
    if (serverRendererTicker != null) {
      serverRendererTicker.getTicker().start();
    }
    box2DTicker.getTicker().start();
  }

  /** Stop this ticker, the tickers thread will not be called anymore */
  @Override
  public void stop() {
    super.stop();
    if (lightTicker != null) {
      lightTicker.getTicker().stop();
    }
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
    if (lightTicker != null) {
      lightTicker.getTicker().pause();
    }
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
    if (lightTicker != null) {
      lightTicker.getTicker().resume();
    }
    if (serverRendererTicker != null) {
      serverRendererTicker.getTicker().resume();
    }
    box2DTicker.getTicker().resume();
  }
}
