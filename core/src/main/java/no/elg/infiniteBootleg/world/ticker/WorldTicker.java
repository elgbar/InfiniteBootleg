package no.elg.infiniteBootleg.world.ticker;

import java.util.Iterator;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.Ticking;
import no.elg.infiniteBootleg.util.Ticker;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * There are multiple tickers for a single world internally, but the outside world should only see a single, main, ticker. When pausing, stopping, resuming the
 * main ticker all slave tickers should do the same.
 * <p>
 * Multiple tickers are needed due to some ticks will happen less frequently.
 */
public class WorldTicker extends Ticker {

    @NotNull
    private final WorldLightTicker lightTicker;
    private final WorldBox2DTicker box2DTicker;

    public WorldTicker(@NotNull World world, boolean tick) {
        super(new WorldTickee(world), "World-" + world.getName(), tick, Settings.tps, Ticker.DEFAULT_NAG_DELAY);
        lightTicker = new WorldLightTicker(world, tick);
        box2DTicker = new WorldBox2DTicker(world, tick);
    }

    private record WorldTickee(World world) implements Ticking {

        @Override
        public void tick() {

            WorldRender wr = world.getRender();
            long chunkUnloadTime = world.getWorldTicker().getTPS() * 5;

            if (world.isSingleplayer()) {
                //tick all chunks and blocks in chunks
                long tick = world.getWorldTicker().getTickId();
                for (Iterator<Chunk> iterator = world.getChunks().values().iterator(); iterator.hasNext(); ) {
                    Chunk chunk = iterator.next();

                    //clean up dead chunks
                    if (!chunk.isLoaded()) {
                        iterator.remove();
                        continue;
                    }
                    //Unload chunks not seen for CHUNK_UNLOAD_TIME
                    if (chunk.isAllowingUnloading() && wr.isOutOfView(chunk) && tick - chunk.getLastViewedTick() > chunkUnloadTime) {

                        world.unloadChunk(chunk);
                        iterator.remove();
                        continue;
                    }
                    chunk.tick();
                }
            }

            for (Entity entity : world.getEntities()) {
                if (entity.isInvalid()) {
                    Main.logger().debug("WORLD", "Invalid entity in world entities (" + entity.simpleName() + ": " + entity.hudDebug() + ")");
                    world.removeEntity(entity);
                    continue;
                }
                entity.tick();
            }
        }

        @Override
        public void tickRare() {
            for (Chunk chunk : world.getLoadedChunks()) {
                chunk.tickRare();
            }
            for (Entity entity : world.getEntities()) {
                if (entity.isInvalid()) {
                    continue;
                }
                entity.tickRare();
            }
        }
    }

    @Override
    public void start() {
        super.start();
        lightTicker.getTicker().start();
        box2DTicker.getTicker().start();
    }

    /**
     * Stop this ticker, the tickers thread will not be called anymore
     */
    @Override
    public void stop() {
        super.stop();
        lightTicker.getTicker().stop();
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
        lightTicker.getTicker().pause();
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
        lightTicker.getTicker().resume();
        box2DTicker.getTicker().resume();
    }
}
