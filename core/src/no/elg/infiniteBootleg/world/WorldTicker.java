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
    public static final long TICKS_PER_MILLISECONDS = 1000L / TICKS_PER_SECOND;

    private long tickId;
    private long frameId;

    private long stuckFrame;
    private boolean running = true;

    public WorldTicker(@NotNull World world) {
        System.out.println("TICKS_PER_MILLISECONDS = " + TICKS_PER_MILLISECONDS);
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
                            if (stuckFrame > TICKS_PER_SECOND * 10) {
                                Main.inst().getConsoleLogger()
                                    .log("Can't keep up! Dropped " + stuckFrame + " world updates!", LogLevel.ERROR);
                            }
                        }
                        frameId = Gdx.graphics.getFrameId();

                        Thread.sleep(TICKS_PER_MILLISECONDS);
                    }
                } catch (InterruptedException ignored) {
                    System.out.println("World updater interrupted");
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
