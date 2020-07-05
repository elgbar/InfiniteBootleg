package no.elg.infiniteBootleg.util;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.strongjoshua.console.LogLevel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import no.elg.infiniteBootleg.Main;
import org.jetbrains.annotations.NotNull;

/**
 * Run (cancellable) tasks on other threads
 *
 * @author kheba
 */
public class CancellableThreadScheduler {

    @NotNull
    private final ScheduledThreadPoolExecutor executor;
    private final Set<ScheduledFuture<?>> tasks;
    private final int threads;

    /**
     * @param threads
     *     The maximum number of threads this scheduler should have.
     *     If less than or equal to zero the number of threads will be equal to {@link Runtime#availableProcessors()}
     */
    public CancellableThreadScheduler(int threads) {
        tasks = ConcurrentHashMap.newKeySet();

        if (threads < 0) {
            threads = Runtime.getRuntime().availableProcessors();
        }
        executor = new ScheduledThreadPoolExecutor(threads, new ThreadPoolExecutor.DiscardPolicy());
        this.threads = threads;
    }

    /**
     * Cancel all future and running tasks
     */
    public void cancelTasks() {
        for (ScheduledFuture<?> sf : tasks) {
            sf.cancel(true);
        }
    }

    public int size() {
        return tasks.size();
    }

    /**
     * Execute a task as soon as possible
     *
     * @param runnable
     *     What to do
     */
    public void executeAsync(@NotNull Runnable runnable) {
        if (isAlwaysSync()) {
            executeSync(runnable);
            return;
        }
        try {
            tasks.add(executor.schedule(caughtRunnable(runnable), 0, TimeUnit.NANOSECONDS));
        } catch (RejectedExecutionException ignored) {
            Main.logger().error("Scheduler", "Runnable rejected from scheduling");
        }
    }

    /**
     * @return If all tasks (even those who should be async) should be executed on the main Gdx thread
     */
    public boolean isAlwaysSync() {
        return threads == 0;
    }

    /**
     * Post the given runnable as fast as possible (though not as fast as calling {@link
     * Application#postRunnable(Runnable)})
     *
     * @param runnable
     *     What to do
     *
     * @see Application#postRunnable(Runnable)
     */
    public void executeSync(@NotNull Runnable runnable) {
        Gdx.app.postRunnable(runnable);
    }

    @NotNull
    private Runnable caughtRunnable(@NotNull Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                Gdx.app.postRunnable(() -> {
                    Main.logger().log(LogLevel.ERROR, "Exception caught on secondary thread");
                    e.printStackTrace();
                });
            }
        };
    }

    /**
     * Run a task in the future async
     *
     * @param runnable
     *     What to do
     * @param ms
     *     How many milliseconds to wait before running the task
     */
    public void scheduleAsync(@NotNull Runnable runnable, long ms) {
        if (isAlwaysSync()) {
            executeSync(runnable);
            return;
        }
        tasks.add(executor.schedule(caughtRunnable(runnable), ms, TimeUnit.MILLISECONDS));
    }


    /**
     * Run a task in the future on the main thread
     *
     * @param runnable
     *     What to do
     * @param ms
     *     How many milliseconds to wait before running the task
     */
    public void scheduleSync(@NotNull Runnable runnable, long ms) {
        tasks.add(executor.schedule(() -> Gdx.app.postRunnable(runnable), ms, TimeUnit.MILLISECONDS));
    }

    /**
     * Shut down the thread
     */
    public void shutdown() {
        executor.shutdownNow();
    }

    public int getActiveThreads() {
        return executor.getActiveCount();
    }
}
