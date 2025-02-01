package no.elg.infiniteBootleg.core.world.ticker

interface PostRunnable {

  /**
   * Add a runnable to be executed on the wanted thread
   *
   * This method is thread safe
   */
  fun postRunnable(runnable: () -> Unit)
}
