package no.elg.infiniteBootleg.util

import com.badlogic.gdx.utils.Timer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ktx.async.AsyncExecutorDispatcher
import ktx.async.KtxAsync
import ktx.async.MainDispatcher
import ktx.async.interval
import ktx.async.newSingleThreadAsyncContext
import ktx.async.schedule
import no.elg.infiniteBootleg.events.api.ThreadType.Companion.currentThreadType
import no.elg.infiniteBootleg.main.Main
import org.jetbrains.annotations.Async
import java.util.concurrent.ScheduledFuture
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

const val ASYNC_THREAD_NAME = "async"
val asyncDispatcher: AsyncExecutorDispatcher = newSingleThreadAsyncContext(ASYNC_THREAD_NAME)

/**
 * Run (cancellable) tasks on other threads
 */
fun launchOnMain(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) = KtxAsync.launch(MainDispatcher, start = start, block = block)

fun launchOnAsync(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) = KtxAsync.launch(asyncDispatcher, start = start, block = block)

fun launchOnWorldTicker(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) =
  KtxAsync.launch(WorldTickCoroutineDispatcher, start = start, block = block)

fun launchOnBox2d(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) =
  KtxAsync.launch(Box2DTickCoroutineDispatcher, start = start, block = block)

object WorldTickCoroutineDispatcher : CoroutineDispatcher() {
  override fun dispatch(context: CoroutineContext, block: Runnable) {
    Main.inst().world?.postWorldTickerRunnable { block.run() }
  }
}

object Box2DTickCoroutineDispatcher : CoroutineDispatcher() {
  override fun dispatch(context: CoroutineContext, block: Runnable) {
    Main.inst().world?.postBox2dRunnable { block.run() }
  }
}

/**
 * Run (cancellable) tasks on other threads
 */
class CancellableThreadScheduler {
//  private val executor: ScheduledExecutorService

//  init {
//    val coreThreads = if (threads < 1) Runtime.getRuntime().availableProcessors() / 2 else threads
//    executor = ScheduledThreadPoolExecutor(coreThreads, Thread.ofVirtual().name("vpool-", 0).factory()) { runnable: Runnable, _: ThreadPoolExecutor ->
//      Gdx.app.postRunnable(runnable)
//    }
//  }

  /**
   * Execute a task as soon as possible
   *
   * @param runnable What to do
   */
  @Deprecated("Use KtxAsync.launch instead", replaceWith = ReplaceWith("launchOnAsync(block = runnable)"))
  fun executeAsync(runnable: Runnable): ScheduledFuture<*>? {
    launchOnAsync { caughtRunnable(runnable).invoke() }
    return null
  }

  /**
   * Post the given runnable as fast as possible
   *
   * @param runnable What to do
   */
  @Deprecated("Use KtxAsync.launch instead", replaceWith = ReplaceWith("launchOnMain(block = runnable)"))
  fun executeSync(runnable: () -> Unit) {
    launchOnMain { runnable() }
  }

  /**
   * Run a task in the future async
   *
   * @param ms How many milliseconds to wait before running the task
   * @param runnable What to do
   */
  @Deprecated("Use KtxAsync.launch instead", replaceWith = ReplaceWith("launchOnAsync { delay(ms)\n runnable }", "kotlinx.coroutines.delay"))
  fun scheduleAsync(ms: Long, runnable: Runnable) {
    launchOnAsync {
      delay(ms)
      runnable.run()
    }
  }

  /**
   * Run a task in the future on the main thread
   *
   * @param ms How many milliseconds to wait before running the task
   * @param runnable What to do
   */
  @Deprecated("Use schedule instead", replaceWith = ReplaceWith("schedule(ms / 1000f, runnable)", "ktx.async.schedule"))
  fun scheduleSync(ms: Long, runnable: () -> Unit): Timer.Task = schedule(ms / 1000f, runnable)

  @Deprecated("Use interval instead", replaceWith = ReplaceWith("interval(delayMs / 1000f, rateMs / 1000f, task = runnable)", "ktx.async.schedule"))
  fun schedulePeriodicSync(delayMs: Long, rateMs: Long, runnable: () -> Unit): Timer.Task = interval(delayMs / 1000f, rateMs / 1000f, task = runnable)

  /** Shut down the thread  */
  @Deprecated("To be removed", level = DeprecationLevel.WARNING)
  fun shutdown() {
  }

  companion object {
    inline fun run(@Async.Execute runnable: Runnable): Unit = runnable.run()

    fun caughtRunnable(@Async.Schedule runnable: Runnable): () -> Unit =
      {
        try {
          run(runnable)
        } catch (e: Exception) {
          logger.info(e) { "Exception caught on " + currentThreadType() }
        }
      }
  }
}
