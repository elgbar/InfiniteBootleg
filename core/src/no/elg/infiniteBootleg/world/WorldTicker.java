package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.Gdx;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.Main;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class WorldTicker {

    public static final long TICKS_PER_SECOND = 30L;
    public static final long MS_DELAY_BETWEEN_TICKS = 1000L / TICKS_PER_SECOND;

    private long tickId;
    private long frameId;

    private long stuckFrame;
    private boolean running = true;

    public WorldTicker(@NotNull World world) {
        Main.inst().getConsoleLogger().logf("TPS: %d", TICKS_PER_SECOND);
        //force save every 30 sec, but not first tick
        Thread worldTickThread = new Thread("World tick thread") {
            @Override
            public void run() {
                try {
                    while (running) {
                        if (frameId != Gdx.graphics.getFrameId()) {
                            if (tickId % TICKS_PER_SECOND == 0) {
                                System.out.println("tick: " + tickId);
                            }
                            //force save every 30 sec, but not first tick
                            //FIXME not currently working (not writing chunks that hasn't been modified since last times
//                            if (tickId > 0 && tickId % (TICKS_PER_SECOND * 10) == 0) {
//                                System.out.println("Saving world " + world.getName() + " (" + world.getUuid() + ")");
//                                Gdx.app.postRunnable(world::save);
//                            }
                            Gdx.app.postRunnable(world::update);
                            tickId++;
                            stuckFrame = 0;
                        }
                        else {
                            stuckFrame++;
                            if (stuckFrame % TICKS_PER_SECOND * 10 == 0) {
                                Main.inst().getConsoleLogger()
                                    .logf(LogLevel.ERROR, "Can't keep up! Failed to update world for %d ticks", stuckFrame);
                                stuckFrame = 0;
                            }
                        }
                        frameId = Gdx.graphics.getFrameId();

                        Thread.sleep(MS_DELAY_BETWEEN_TICKS);
                    }
                } catch (InterruptedException ignored) {
                    Main.inst().getConsoleLogger().log("World updater interrupted");
                }
            }
        };
        worldTickThread.setDaemon(true);
        worldTickThread.start();
    }

    public long getTickId() {
        return tickId;
    }

    public void stop() {
        running = false;
    }
}
