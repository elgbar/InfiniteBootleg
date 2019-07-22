package no.elg.infiniteBootleg.util;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.Main;

import java.util.Set;
import java.util.concurrent.*;

/**
 * Run (cancellable) tasks on another thread
 *
 * @author kheba
 */
public class CancellableThreadScheduler {

    private final ScheduledExecutorService executorService;
    private final Set<ScheduledFuture> tasks;
    private final int threads;

    public CancellableThreadScheduler(int threads) {
        this.threads = threads;
        if (threads <= 1) {
            executorService = Executors.newSingleThreadScheduledExecutor();
        }
        else {
            executorService = Executors.newScheduledThreadPool(threads);
        }
        tasks = ConcurrentHashMap.newKeySet();
    }

    /**
     * Cancel all future and running tasks
     */
    public void cancelTasks() {
        for (ScheduledFuture sf : tasks) {
            sf.cancel(true);
        }
    }

    public int size() {
        return tasks.size();
    }

    private Runnable caughtRunnable(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                Gdx.app.postRunnable(() -> {
                    Main.inst().getConsoleLogger().log(LogLevel.ERROR, "Exception caught on secondary thread");
                    e.printStackTrace();
                });
            }
        };
    }

    /**
     * Execute a task as soon as possible
     *
     * @param runnable
     *     What to do
     */
    public void executeAsync(Runnable runnable) {
        if (threads <= 0) {
            executeSync(runnable);
            return;
        }
        tasks.add(executorService.schedule(caughtRunnable(runnable), 0, TimeUnit.NANOSECONDS));
    }

    /**
     * Post the given runnable as fast as possible (though not as fast as calling {@link Application#postRunnable(Runnable)})
     * <p>
     * This is NOT the same as doing {@code Gdx.app.postRunnable(runnable)}
     *
     * @param runnable
     *     What to do
     *
     * @see Application#postRunnable(Runnable)
     */
    public void executeSync(Runnable runnable) {
        tasks.add(executorService.schedule(() -> Gdx.app.postRunnable(runnable), 0, TimeUnit.NANOSECONDS));
    }

    /**
     * Run a task in the future async
     *
     * @param runnable
     *     What to do
     * @param ms
     *     How many milliseconds to wait before running the task
     */
    public void scheduleAsync(Runnable runnable, long ms) {
        if (threads <= 0) {
            executeSync(runnable);
            return;
        }
        tasks.add(executorService.schedule(caughtRunnable(runnable), ms, TimeUnit.MILLISECONDS));
    }


    /**
     * Run a task in the future on the main thread
     *
     * @param runnable
     *     What to do
     * @param ms
     *     How many milliseconds to wait before running the task
     */
    public void scheduleSync(Runnable runnable, long ms) {
        tasks.add(executorService.schedule(() -> Gdx.app.postRunnable(runnable), ms, TimeUnit.MILLISECONDS));
    }

    /**
     * Shut down the thread
     */
    public void shutdown() {
        executorService.shutdownNow();
    }
}
