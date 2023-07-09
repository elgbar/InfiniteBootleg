package no.elg.infiniteBootleg.world.ticker

import com.badlogic.gdx.utils.Array
import ktx.collections.GdxArray

class PostRunnableHandler : PostRunnable {

  private val runnables = GdxArray<Runnable>()
  private val executedRunnables = GdxArray<Runnable>()
  private val executedRunnablesIterator = Array.ArrayIterator(executedRunnables)

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
      runnable.run()
    }
  }
}
