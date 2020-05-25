package no.elg.infiniteBootleg.util;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.utils.PauseableThread;
import com.badlogic.gdx.utils.TimeUtils;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Ticking;
import org.jetbrains.annotations.NotNull;

/**
 * A helper class that calls a {@link Ticking}'s {@link Ticking#tick()} and {@link Ticking#tickRare()} method
 * periodically. By
 * default it will call it every {@link #msDelayBetweenTicks}.
 * <p>
 * The ticker will not update the if {@link Graphics#getFrameId()} is the same as it was last tick
 * (as can be caused by fps lag), and will warn when too many ticks have been skipped.
 *
 * @author Elg
 */
public class Ticker implements Runnable {

    public static final long DEFAULT_TICKS_PER_SECOND = 20L;
    /**
     * How many seconds to wait (based on the tps) between each lag message
     */
    public static final float DEFAULT_NAG_DELAY = 3f;

    private final PauseableThread tickerThread;
    private final Ticking ticking;

    /**
     * The ticks per seconds this ticker is using. Defaults to {@link #DEFAULT_TICKS_PER_SECOND}
     */
    private final long tps;

    private final long msDelayBetweenTicks;
    private final long nanoDelayBetweenTicks;
    private final float secondsDelayBetweenTicks;
    private final float nagDelay;

    /**
     * How many ticks between each rare update. Currently each rare tick is the same as one second
     */
    public final long tickRareRate;

    /**
     * The current tick
     */
    private long tickId;

    /**
     * The current ticks per second
     */
    private long realTPS = -1;

    /**
     * At what tick did we start the tps calculation
     */
    private long tpsCalcStartTime;
    /**
     * Ticks accumulated during calculating tps
     */
    private long tpsTick;
    /**
     * Last diff between each tick
     */
    private long tpsDelta;

    /**
     * How long ago we last showed a "can't keep up" warning
     */
    private long lastNagged;


    /**
     * Use {@link #DEFAULT_TICKS_PER_SECOND} for {@code #tps} and {@link #DEFAULT_NAG_DELAY} for nag delay
     *
     * @param ticking
     *     The ticker to tick
     * @param start
     *     If the thread should start at once
     */
    public Ticker(@NotNull Ticking ticking, boolean start) {
        this(ticking, start, DEFAULT_TICKS_PER_SECOND, DEFAULT_NAG_DELAY);
    }

    /**
     * @param ticking
     *     The ticker to tick
     * @param start
     *     If the thread should start at once
     * @param tps
     *     Ticks per seconds, must be a strictly positive number
     * @param nagDelay
     *     Minimum seconds between each nag message, If less than or equal to zero there will be no delay (note that
     *     this will be a lot of spam!)
     *
     * @see #DEFAULT_TICKS_PER_SECOND
     * @see #DEFAULT_NAG_DELAY
     */
    public Ticker(@NotNull Ticking ticking, boolean start, long tps, float nagDelay) {
        this.ticking = ticking;
        if (tps <= 0) {
            throw new IllegalArgumentException("TPS must be strictly positive! Was given " + tps);
        }
        this.tps = tps;

        msDelayBetweenTicks = 1000L / tps;
        nanoDelayBetweenTicks = 1_000_000_000L / tps;
        secondsDelayBetweenTicks = 1f / tps;
        tickRareRate = this.tps;
        this.nagDelay = Math.max(0, tps * nagDelay);

        Main.logger().debug("TICK", "Starting ticking thread for '" + ticking + "' with TPS = " + tps);

        tickerThread = new PauseableThread(this);
        tickerThread.setName("Ticker");
        tickerThread.setDaemon(true);

        if (start) {
            //Do not begin ticking until the render thread is finish initialization
            Main.inst().getScheduler().executeSync(tickerThread::start);
        }
    }

    @Override
    public void run() {
        long start = TimeUtils.nanoTime();
        try {
            ticking.tick();
            if (tickId % tickRareRate == 0) {
                ticking.tickRare();
            }
        } catch (Exception e) {
            Main.logger().error("TICK", "Failed to tick " + ticking, e);
        }
        tickId++;

        //Calculate ticks per second once per second
        if (start - tpsCalcStartTime >= 1_000_000_000) {
            realTPS = tpsTick;
            tpsTick = 0;
            tpsCalcStartTime = start;
        }
        tpsTick++;

        tpsDelta = TimeUtils.nanoTime() - start;
        long ms = msDelayBetweenTicks - TimeUtils.nanosToMillis(tpsDelta);

        if (ms > 0) {
            try {
                int nano = (int) tpsDelta % 1_000_000; // There are one million nano second in a millisecond
                Thread.sleep(ms, nano);
            } catch (InterruptedException e) {
                Main.logger().error("TICK", "World ticker interrupted");
            }
        }
        else if (tickId - lastNagged >= nagDelay) {
            lastNagged = tickId;
            Main.logger().error("TICK", "Cant keep up a single tick took around " + TimeUtils.nanosToMillis(tpsDelta) +
                                        " ms, while at max it should take " + msDelayBetweenTicks + " ms");
        }
    }

    /**
     * @return How many ticks have passed since start
     */
    public long getTickId() {
        return tickId;
    }


    /**
     * @return The current TPS might differ from {@link #DEFAULT_TICKS_PER_SECOND} but will never be greater than it
     */
    public long getRealTPS() {
        return realTPS;
    }

    public long getTPS() {
        return tps;
    }

    public long getMsDelayBetweenTicks() {
        return msDelayBetweenTicks;
    }

    public long getNanoDelayBetweenTicks() {
        return nanoDelayBetweenTicks;
    }

    public float getSecondsDelayBetweenTicks() {
        return secondsDelayBetweenTicks;
    }

    public float getNagDelay() {
        return nagDelay;
    }

    public long getTickRareRate() {
        return tickRareRate;
    }

    /**
     * @return Nanoseconds between each tick
     */
    public long getTpsDelta() {
        return tpsDelta;
    }

    /**
     * Stop this ticker, the tickers thread will not be called anymore
     */
    public void stop() {
        tickerThread.stopThread();
    }

    /**
     * Temporarily stops this ticker, can be resumed with {@link #resume()}
     *
     * @see #isPaused()
     * @see #resume()
     */
    public void pause() {
        tickerThread.onPause();
    }

    /**
     * Resume the ticking thread if it {@link #isPaused()}
     *
     * @see #isPaused()
     * @see #pause()
     */
    public void resume() {
        tickerThread.onResume();
    }

    /**
     * @return If this ticker thread is paused
     *
     * @see #pause()
     * @see #resume()
     */
    public boolean isPaused() {
        return tickerThread.isPaused();
    }
}
