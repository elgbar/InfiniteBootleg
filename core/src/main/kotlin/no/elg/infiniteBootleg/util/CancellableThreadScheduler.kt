package no.elg.infiniteBootleg.util

import com.badlogic.gdx.Gdx
import com.google.common.base.Preconditions
import no.elg.infiniteBootleg.events.api.ThreadType
import no.elg.infiniteBootleg.events.api.ThreadType.Companion.currentThreadType
import no.elg.infiniteBootleg.main.Main.Companion.logger
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Run (cancellable) tasks on other threads
 */
class CancellableThreadScheduler(threads: Int) {
  private val executor: ScheduledThreadPoolExecutor

  init {
    val coreThreads = threads.coerceAtLeast(1)
    executor = ScheduledThreadPoolExecutor(coreThreads) { runnable: Runnable, _: ThreadPoolExecutor ->
      Gdx.app.postRunnable(runnable)
    }
  }

  private val activeThreads: Int get() = executor.getActiveCount()

  /**
   * Block until all scheduled tasks have been completed. Must be on the main thread, and not hold
   * any locks
   */
  @Deprecated("This method is not safe to use")
  fun waitForTasks() {
    Preconditions.checkState(ThreadType.currentThreadType() == ThreadType.ASYNC, "Can only wait for tasks on main thread")
    while (activeThreads > 0) {
      Thread.onSpinWait()
    }
  }

  /**
   * Execute a task as soon as possible
   *
   * @param runnable What to do
   */
  fun executeAsync(runnable: Runnable): ScheduledFuture<*>? {
    if (isAlwaysSync) {
      executeSync(runnable)
      return null
    }
    return executor.schedule(caughtRunnable(runnable), 0, TimeUnit.NANOSECONDS)
  }

  /**
   * @return If all tasks (even those who should be async) should be executed on the main Gdx thread
   */
  private val isAlwaysSync: Boolean = threads == 0

  /**
   * Post the given runnable as fast as possible (though not as fast as calling [ ][Application.postRunnable])
   *
   * @param runnable What to do
   * @see Application.postRunnable
   */
  fun executeSync(runnable: Runnable) {
    Gdx.app.postRunnable(caughtRunnable(runnable))
  }

  /**
   * Run a task in the future async
   *
   * @param ms How many milliseconds to wait before running the task
   * @param runnable What to do
   */
  fun scheduleAsync(ms: Long, runnable: Runnable): ScheduledFuture<*> {
    return if (isAlwaysSync) {
      scheduleSync(ms, runnable)
    } else {
      executor.schedule(caughtRunnable(runnable), ms, TimeUnit.MILLISECONDS)
    }
  }

  /**
   * Run a task in the future on the main thread
   *
   * @param ms How many milliseconds to wait before running the task
   * @param runnable What to do
   */
  fun scheduleSync(ms: Long, runnable: Runnable): ScheduledFuture<*> = executor.schedule({ Gdx.app.postRunnable(runnable) }, ms, TimeUnit.MILLISECONDS)

  fun schedulePeriodicSync(delayMs: Long, rateMs: Long, runnable: Runnable): ScheduledFuture<*> =
    executor.scheduleWithFixedDelay(
      { Gdx.app.postRunnable(runnable) },
      delayMs,
      rateMs,
      TimeUnit.MILLISECONDS
    )

  /** Shut down the thread  */
  fun shutdown() {
    executor.shutdown()
    try {
      if (!executor.awaitTermination(1L, TimeUnit.SECONDS)) {
        executor.shutdownNow()
      }
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  companion object {
    private fun caughtRunnable(runnable: Runnable): Runnable =
      Runnable {
        try {
          runnable.run()
        } catch (e: Exception) {
          logger().log("SCHEDULER", "Exception caught on " + currentThreadType(), e)
        }
      }
  }
}
