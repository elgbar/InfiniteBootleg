package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.utils.PauseableThread;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.Main;
import org.jetbrains.annotations.NotNull;

/**
 * A helper class that calls the world's {@link World#update()} method periodically. By default it will call it every {@link
 * #MS_DELAY_BETWEEN_TICKS}.
 * <p>
 * The world ticker will not update the world if {@link Graphics#getFrameId()} is the same as it was last tick
 * (as can be caused by fps lag), and will warn when too many world ticks have been skipped.
 *
 * @author Elg
 */
public class WorldTicker {

    public static final long TICKS_PER_SECOND = 60L;
    public static final long MS_DELAY_BETWEEN_TICKS = 1000L / TICKS_PER_SECOND;
    public static final float SECONDS_DELAY_BETWEEN_TICKS = 1f / TICKS_PER_SECOND;
    private final PauseableThread worldTickThread;

    private long tickId;
    private long frameId;

    private long stuckFrames;

    public WorldTicker(@NotNull World world) {
        Main.inst().getConsoleLogger().log("TPS: " + TICKS_PER_SECOND);
        worldTickThread = new PauseableThread(() -> {
            try {
                if (frameId != Gdx.graphics.getFrameId()) {
//                            if (tickId % TICKS_PER_SECOND == 0) {
////                                System.out.println("tick: " + tickId);
//                            }
                    //force save every 30 sec, but not first tick
                    //FIXME not currently working (not writing chunks that hasn't been modified since last times
//                            if (tickId > 0 && tickId % (TICKS_PER_SECOND * 10) == 0) {
//                                System.out.println("Saving world " + world.getName() + " (" + world.getUuid() + ")");
//                                Gdx.app.postRunnable(world::save);
//                            }
                    Gdx.app.postRunnable(world::update);
                    tickId++;
                    stuckFrames = 0;
                    frameId = Gdx.graphics.getFrameId();
                }
                else {
                    stuckFrames++;
                    if (stuckFrames == TICKS_PER_SECOND || stuckFrames % (TICKS_PER_SECOND * 10) == 0) {
                        Main.inst().getConsoleLogger()
                            .logf(LogLevel.ERROR, "Can't keep up! Failed to update world for %d ticks", stuckFrames);
                    }
                }
                Thread.sleep(MS_DELAY_BETWEEN_TICKS);
            } catch (InterruptedException ignored) {
                Main.inst().getConsoleLogger().log("World updater interrupted");
            }

        });
        worldTickThread.setName("World tick thread");
        worldTickThread.setDaemon(true);
        worldTickThread.start();
    }

    /**
     * @return How many times the world have been updated since start
     */
    public long getTickId() {
        return tickId;
    }

    /**
     * Stop this world ticker, world's {@link World#update()} method will no longer be called
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
