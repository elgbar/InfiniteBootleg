package no.elg.infiniteBootleg.core.world.ticker

import com.badlogic.gdx.utils.Array.ArrayIterator
import ktx.collections.GdxArray
import no.elg.infiniteBootleg.core.util.IllegalAction
import org.jetbrains.annotations.Async

class PostRunnableHandler : PostRunnable {

  private val runnables = GdxArray<() -> Unit>()
  private val executedRunnables = GdxArray<() -> Unit>()
  private val executedRunnablesIterator = ArrayIterator(executedRunnables)

  override fun postRunnable(@Async.Schedule runnable: () -> Unit) {
    synchronized(runnables) {
      runnables.add(runnable)
    }
  }

  fun hasRunnables(): Boolean = runnables.size > 0

  /**
   * Execute all runnables that have been posted since the last time this method was called.
   * This method is NOT thread safe, so should always be called on a single thread
   */
  fun executeRunnables() {
    synchronized(runnables) {
      executedRunnables.clear()
      executedRunnables.addAll(runnables)
      runnables.clear()
    }
    executedRunnablesIterator.reset()
    for (runnable in executedRunnablesIterator) {
      execute(runnable)
    }
  }

  fun execute(@Async.Execute runnable: () -> Unit) {
    try {
      runnable()
    } catch (e: Exception) {
      IllegalAction.LOG.handle(e) { "Failed to run task" }
    }
  }
}
