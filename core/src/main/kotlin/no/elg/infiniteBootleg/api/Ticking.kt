package no.elg.infiniteBootleg.api

/**
 * @author Elg
 */
interface Ticking {
  /**
   * Tick an object.
   *
   * You might want to use synchronization either on the whole method or parts of the method to
   * ensure correctness
   */
  fun tick()

  /**
   * Update rarely (for expensive methods) this should be called every [Ticker.getTickRareRate] ticks.
   *
   *
   * You might want to use synchronization either on the whole method or parts of the method to
   * ensure correctness
   */
  fun tickRare() {}
}
