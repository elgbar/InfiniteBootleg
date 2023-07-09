package no.elg.infiniteBootleg.world.ticker

interface Ticker : PostRunnable {
  /** The ticks per seconds this ticker is using. Defaults to [.DEFAULT_TICKS_PER_SECOND]  */
  val tps: Long
  val secondsDelayBetweenTicks: Float

  /** The current tick  */
  val tickId: Long

  /** The current ticks per second  */
  val realTPS: Long

  /** Last diff between each tick  */
  val tpsDelta: Long

  val isPaused: Boolean

  val isStarted: Boolean

  fun start()

  /** Stop this ticker, the tickers thread will not be called anymore  */
  fun stop()

  /**
   * Temporarily stops this ticker, can be resumed with [.resume]
   *
   * @see .isPaused
   * @see .resume
   */
  fun pause()

  /**
   * Resume the ticking thread if it [.isPaused]
   *
   * @see .isPaused
   * @see .pause
   */
  fun resume()
}
