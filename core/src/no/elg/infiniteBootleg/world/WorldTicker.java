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

    public static final long TICKS_PER_SECOND = 20L;
    public static final long MS_DELAY_BETWEEN_TICKS = 1000L / TICKS_PER_SECOND;
    public static final long NANO_DELAY_BETWEEN_TICKS = 1_000_000_000L / TICKS_PER_SECOND;
    public static final float SECONDS_DELAY_BETWEEN_TICKS = 1f / TICKS_PER_SECOND;

    public static final long NAG_DELAY_TICKS = TICKS_PER_SECOND * 3;

    private final PauseableThread worldTickThread;
    private final World world;

    private long tickId;            //The current tick
    private long tps = -1;          //The current ticks per second
    private long tpsCalcStartTime;  //At what tick did we start the tps calculation
    private long tpsTick;           //Ticks accumulated during calculating tps
    private long tpsDelta;

    private long lastNagged; //How long ago we last showed a 'cant keep up' warning

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

        //Calculate ticks per second once per second
        if (start - tpsCalcStartTime >= 1_000_000_000) {
            tps = tpsTick;
            tpsTick = 0;
            tpsCalcStartTime = start;
        }
        tpsTick++;

        tpsDelta = TimeUtils.nanoTime() - start;
        long ms = MS_DELAY_BETWEEN_TICKS - TimeUtils.nanosToMillis(tpsDelta);

        if (ms > 0) {
            try {
                int nano = (int) tpsDelta % 1_000_000; // There are one million nano second in a millisecond
                Thread.sleep(ms, nano);
            } catch (InterruptedException e) {
                Main.logger().error("TICK", "World ticker interrupted");
            }
        }
        else if (tickId - lastNagged >= NAG_DELAY_TICKS) {
            lastNagged = tickId;
            Main.logger().error("TICK", "Cant keep up a single tick took around " + TimeUtils.nanosToMillis(tpsDelta) +
                                        " ms, while at max it should take " + MS_DELAY_BETWEEN_TICKS + " ms");
        }
    }


    /**
     * @return How many times the world have been updated since start
     */
    public long getTickId() {
        return tickId;
    }


    /**
     * @return The current TPS might differ from {@link #TICKS_PER_SECOND} but will never be greater than it
     */
    public long getRealTPS() {
        return tps;
    }

    /**
     * @return Nanoseconds between each tick
     */
    public long getTpsDelta() {
        return tpsDelta;
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
