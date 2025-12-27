package no.elg.infiniteBootleg.core.world.ticker

import no.elg.infiniteBootleg.core.util.CheckableDisposable

interface Ticker :
  PostRunnable,
  CheckableDisposable {
  /** The ticks per seconds this ticker is using. Defaults to [TickerImpl.DEFAULT_TICKS_PER_SECOND]  */
  val tps: Long
  val secondsDelayBetweenTicks: Float

  /**
   * @return How many ticks have passed since start
   */
  val tickId: Long

  /**
   * @return The current TPS might differ from [TickerImpl.DEFAULT_TICKS_PER_SECOND] but will never be greater than it
   */
  val realTPS: Long

  /**
   * @return Nanoseconds between each tick
   */
  val tpsDelta: Long

  /**
   * @return Whether this ticker thread is paused or has not started yet
   *
   * @see pause
   * @see resume
   */
  val isPaused: Boolean

  /**
   * If the ticker has been started
   */
  val isStarted: Boolean

  fun start()

  /**
   * Temporarily stops this ticker, can be resumed with [resume]
   *
   * @see isPaused
   * @see resume
   */
  fun pause()

  /**
   * Resume the ticking thread if it [isPaused]
   *
   * @see isPaused
   * @see pause
   */
  fun resume()
}
