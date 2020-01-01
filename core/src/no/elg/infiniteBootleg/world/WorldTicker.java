package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.utils.PauseableThread;
import com.badlogic.gdx.utils.TimeUtils;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.render.Ticking;
import org.jetbrains.annotations.NotNull;

/**
 * A helper class that calls the world's {@link World#tick()} and {@link World#tickRare()} method periodically. By
 * default it will call it every {@link #MS_DELAY_BETWEEN_TICKS}.
 * <p>
 * The world ticker will not update the world if {@link Graphics#getFrameId()} is the same as it was last tick
 * (as can be caused by fps lag), and will warn when too many world ticks have been skipped.
 *
 * @author Elg
 */
public class WorldTicker implements Runnable {

    public static final long TICKS_PER_SECOND = 30L;
    public static final long MS_DELAY_BETWEEN_TICKS = 1000L / TICKS_PER_SECOND;
    public static final long NANO_DELAY_BETWEEN_TICKS = 1_000_000_000L / TICKS_PER_SECOND;
    public static final float SECONDS_DELAY_BETWEEN_TICKS = 1f / TICKS_PER_SECOND;
    private final PauseableThread worldTickThread;
    private final World world;

    private long tickId;
    private long tps = -1;
    private long startTime;
    private long tpsTick;

    public WorldTicker(@NotNull World world, boolean start) {
        this.world = world;
        Main.logger().log("Starting world ticking thread with TPS = " + TICKS_PER_SECOND);

        worldTickThread = new PauseableThread(this);
        worldTickThread.setName("World Ticker");
        worldTickThread.setDaemon(true);

        if (start) {
            //Do not begin ticking until the render thread is finish initialization
            Main.inst().getScheduler().executeSync(worldTickThread::start);
        }
    }

    @Override
    public void run() {
        long start = TimeUtils.nanoTime();
        try {
            world.tick();
            if (tickId % Ticking.TICK_RARE_RATE == 0) {
                world.tickRare();
            }
        } catch (Exception e) {
            Main.logger().error("TICK", "Failed to tick world " + world.getName(), e);
        }
        tickId++;

        //Calculate ticks per second
        if (start - startTime >= 1000000000) {
            tps = tpsTick;
            tpsTick = 0;
            startTime = start;
        }
        tpsTick++;

        long totalNanos = TimeUtils.nanoTime() - start;
        long ms = MS_DELAY_BETWEEN_TICKS - TimeUtils.nanosToMillis(totalNanos);

        if (ms > 0) {
            try {
                int nano = (int) (totalNanos % 1_000_000); // There are one million nano second in a millisecond
                Thread.sleep(ms, nano);
            } catch (InterruptedException e) {
                Main.logger().error("TICK", "World ticker interrupted");
            }
        }
        else {
            Main.logger().error("TICK",
                                "Cant keep up a single tick took around " + TimeUtils.nanosToMillis(totalNanos) +
                                " ms, while at max it should take " + MS_DELAY_BETWEEN_TICKS + " ms");
        }
    }


    /**
     * @return How many times the world have been updated since start
     */
    public long getTickId() {
        return tickId;
    }


    public long getRealTPS() {
        return tps;
    }

    /**
     * Stop this world ticker, world's {@link World#tick()} method will no longer be called
     */
    public void stop() {
        worldTickThread.stopThread();
    }

    public void pause() {
        worldTickThread.onPause();
    }

    public void resume() {
        worldTickThread.onResume();
    }

    public boolean isPaused() {
        return worldTickThread.isPaused();
    }
}
