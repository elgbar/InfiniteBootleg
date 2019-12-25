package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.utils.PauseableThread;
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

    public static final long TICKS_PER_SECOND = 60L;
    public static final long MS_DELAY_BETWEEN_TICKS = 1000L / TICKS_PER_SECOND;
    public static final float SECONDS_DELAY_BETWEEN_TICKS = 1f / TICKS_PER_SECOND;
    private final PauseableThread worldTickThread;
    private final World world;

    private long tickId;

    public WorldTicker(@NotNull World world) {
        this.world = world;
        Main.logger().log("Starting world ticking thread with TPS = " + TICKS_PER_SECOND);
        worldTickThread = new PauseableThread(this);
        worldTickThread.setName("World Ticker");
        worldTickThread.setDaemon(true);
        worldTickThread.start();
    }

    @Override
    public void run() {
        try {

            Gdx.app.postRunnable(world::tick);
            if (tickId % Ticking.TICK_RARE_RATE == 0) {
                Gdx.app.postRunnable(world::tickRare);
            }
            tickId++;
            Thread.sleep(MS_DELAY_BETWEEN_TICKS);
        } catch (InterruptedException ignored) {
            Main.logger().log("World updater interrupted");
        }
    }


    /**
     * @return How many times the world have been updated since start
     */
    public long getTickId() {
        return tickId;
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
