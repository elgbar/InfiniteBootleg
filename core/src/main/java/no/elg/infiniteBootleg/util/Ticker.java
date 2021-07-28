package no.elg.infiniteBootleg.util;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.utils.PauseableThread;
import com.badlogic.gdx.utils.TimeUtils;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Ticking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public static final long DEFAULT_TICKS_PER_SECOND = 60L;
    /**
     * How many seconds to wait (based on the tps) between each lag message
     */
    public static final double DEFAULT_NAG_DELAY = 3d;
    /**
     * How low, in percent (0f..1f), the real tps must reach for the user to be warned
     */
    public static final float TPS_LOSS_PERCENTAGE = 0.94f;

    /**
     * How many ticks between each rare update. Currently each rare tick is the same as one second
     */
    public final long tickRareRate;
    private final PauseableThread tickerThread;
    private final Ticking ticking;
    private final String tag;
    /**
     * The ticks per seconds this ticker is using. Defaults to {@link #DEFAULT_TICKS_PER_SECOND}
     */
    private final long tps;
    private final long msDelayBetweenTicks;
    private final long nanoDelayBetweenTicks;
    private final float secondsDelayBetweenTicks;
    private final long nagDelayTicks;

    /**
     * How low the tps must reach before displaying a "can't keep up" warning
     */
    private final long tpsWarnThreshold;

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
    private long lastTickNagged;


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
    public Ticker(@NotNull Ticking ticking, boolean start, long tps, double nagDelay) {
        this(ticking, null, start, tps, nagDelay);
    }

    /**
     * @param ticking
     *     The ticker to tick
     * @param name
     *     Name of the ticker thread
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
    public Ticker(@NotNull Ticking ticking, @Nullable String name, boolean start, long tps, double nagDelay) {
        this.ticking = ticking;
        tag = ((name != null) ? name : ticking.toString()) + " ticker";
        if (tps <= 0) {
            throw new IllegalArgumentException("TPS must be strictly positive! Was given " + tps);
        }
        this.tps = tps;

        msDelayBetweenTicks = 1000L / tps;
        nanoDelayBetweenTicks = 1_000_000_000L / tps;
        secondsDelayBetweenTicks = 1f / tps;
        tickRareRate = this.tps;
        // Round down ok as realTPS is a long
        tpsWarnThreshold = (long) (TPS_LOSS_PERCENTAGE * tps);
        nagDelayTicks = (long) Math.max(0, tps * nagDelay);

        Main.logger().debug(tag, "Starting ticking thread for '" + name + "' with tps = " + tps + " (warn when tps <= " + tpsWarnThreshold + ")");

        tickerThread = new PauseableThread(this);
        tickerThread.setName(tag);
        tickerThread.setDaemon(true);

        if (start) {
            //Do not begin ticking until the render thread is initialized
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
        } catch (Throwable e) {
            Main.logger().error(tag, "Failed to tick", e);
        }
        tickId++;

        //Calculate ticks per second once per second
        if (start - tpsCalcStartTime >= 1_000_000_000) {
            // tps is number of times we ticked.
            //  We calculate once per second, so no dividing is required
            realTPS = tpsTick;
            tpsTick = 0;
            tpsCalcStartTime = start;
        }
        tpsTick++;

        tpsDelta = TimeUtils.nanoTime() - start;
        long ms = msDelayBetweenTicks - TimeUtils.nanosToMillis(tpsDelta);


        if (ms > 0) {
            int nano = (int) tpsDelta % 1_000_000; // There are one million nano second in a millisecond
            try {
                Thread.sleep(ms, nano);
            } catch (InterruptedException e) {
                Main.logger().error(tag, "Ticker interrupted");
            }
        }
        else if (tickId - lastTickNagged >= nagDelayTicks && tpsWarnThreshold >= realTPS) {
            lastTickNagged = tickId;
            Main.logger().error(tag, "Cant keep up a single tick took around " + TimeUtils.nanosToMillis(tpsDelta) + " ms, while at max it should take " +
                                     getMsDelayBetweenTicks() + " ms " + getNanoDelayBetweenTicks() + " ns");
        }
    }

    public long getMsDelayBetweenTicks() {
        return msDelayBetweenTicks;
    }

    public long getNanoDelayBetweenTicks() {
        return nanoDelayBetweenTicks;
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

    public float getSecondsDelayBetweenTicks() {
        return secondsDelayBetweenTicks;
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
