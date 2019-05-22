package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class WorldTicker implements Disposable {

    public static final long TICKS_PER_SECOND = 30L;
    public static final long TICKS_PER_MILLISECONDS = 1000L / TICKS_PER_SECOND;

    private final Thread worldTickThread;

    private long tickId;

    public WorldTicker(@NotNull World world) {
        System.out.println("TICKS_PER_MILLISECONDS = " + TICKS_PER_MILLISECONDS);
        worldTickThread = new Thread("World tick thread") {
            @Override
            public void run() {
                try {
                    //noinspection InfiniteLoopStatement
                    while (true) {
                        if (tickId % TICKS_PER_SECOND == 0) {
                            System.out.println("tick: " + tickId);
                        }
                        Gdx.app.postRunnable(world::update);
                        tickId++;

                        Thread.sleep(TICKS_PER_MILLISECONDS);
                    }
                } catch (InterruptedException ignored) {
                    System.out.println("World updater stopped");
                }
            }
        };
        worldTickThread.start();
    }

    public long getTickId() {
        return tickId;
    }

    @Override
    public void dispose() {
        worldTickThread.interrupt();
    }
}
