package no.elg.infiniteBootleg.core.world.ticker

import com.badlogic.gdx.utils.Array.ArrayIterator
import ktx.collections.GdxArray
import no.elg.infiniteBootleg.core.util.IllegalAction
import kotlin.Exception
import kotlin.Unit
import kotlin.synchronized

class PostRunnableHandler : PostRunnable {

  private val runnables = GdxArray<Runnable>()
  private val executedRunnables = GdxArray<Runnable>()
  private val executedRunnablesIterator = ArrayIterator(executedRunnables)

  override fun postRunnable(runnable: () -> Unit) {
    synchronized(runnables) {
      runnables.add(runnable)
    }
  }

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
      try {
        runnable.run()
      } catch (e: Exception) {
        IllegalAction.LOG.handle(e) { "Failed to run task" }
      }
    }
  }
}
