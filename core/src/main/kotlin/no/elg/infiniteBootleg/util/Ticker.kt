package no.elg.infiniteBootleg.util

import com.badlogic.gdx.utils.PauseableThread
import com.badlogic.gdx.utils.TimeUtils
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.api.Ticking
import no.elg.infiniteBootleg.events.api.ThreadType
import no.elg.infiniteBootleg.events.api.ThreadType.Companion.currentThreadType
import no.elg.infiniteBootleg.exceptions.CalledFromWrongThreadTypeException

/**
 * A helper class that calls a [Ticking]'s [Ticking.tick] and [ ][Ticking.tickRare] method periodically. By default it will call it every [ ][.msDelayBetweenTicks].
 *
 *
 * The ticker will not update the if [Graphics.getFrameId] is the same as it was last
 * tick (as can be caused by fps lag), and will warn when too many ticks have been skipped.
 *
 * @author Elg
 */
open class Ticker(
  private val ticking: Ticking,
  name: String,
  start: Boolean,
  tps: Long,
  nagDelay: Double
) : Runnable {
  /**
   * How many ticks between each rare update. Currently, each rare tick is the same as one second
   */
  private val tickRareRate: Long
  private val tickerThread: PauseableThread
  private val tag: String

  /** The ticks per seconds this ticker is using. Defaults to [.DEFAULT_TICKS_PER_SECOND]  */
  val tps: Long
  val secondsDelayBetweenTicks: Float
  private val msDelayBetweenTicks: Long
  private val nanoDelayBetweenTicks: Long
  private val nagDelayTicks: Long

  /** How low the tps must reach before displaying a "can't keep up" warning  */
  private val tpsWarnThreshold: Long
  /**
   * @return How many ticks have passed since start
   */
  /** The current tick  */
  var tickId: Long = 0
    private set
  /**
   * @return The current TPS might differ from [.DEFAULT_TICKS_PER_SECOND] but will never be
   * greater than it
   */
  /** The current ticks per second  */
  var realTPS: Long = -1
    private set

  /** At what tick did we start the tps calculation  */
  private var tpsCalcStartTime: Long = 0

  /** Ticks accumulated during calculating tps  */
  private var tpsTick: Long = 0
  /**
   * @return Nanoseconds between each tick
   */
  /** Last diff between each tick  */
  var tpsDelta: Long = 0
    private set

  /** How long ago we last showed a "can't keep up" warning  */
  private var lastTickNagged: Long = 0

  @Volatile
  var isStarted = false
    private set

  /**
   * @param ticking The ticker to tick
   * @param name Name of the ticker thread
   * @param start If the thread should start at once
   * @param tps Ticks per seconds, must be a strictly positive number
   * @param nagDelay Minimum seconds between each nag message, If less than or equal to zero there
   * will be no delay (note that this will be a lot of spam!)
   */
  init {
    tag = "$name ticker"
    require(tps > 0) { "TPS must be strictly positive! Was given $tps" }
    this.tps = tps
    msDelayBetweenTicks = ONE_SECOND_MILLIS / tps
    nanoDelayBetweenTicks = ONE_SECOND_NANO / tps
    secondsDelayBetweenTicks = 1f / tps
    tickRareRate = this.tps
    // Round down ok as realTPS is a long
    tpsWarnThreshold = (TPS_LOSS_PERCENTAGE * tps).toLong()
    nagDelayTicks = Math.max(0.0, tps * nagDelay).toLong()
    Main.logger()
      .debug(
        tag,
        "Starting ticking thread for '" +
          name +
          "' with tps = " +
          tps +
          " (warn when tps <= " +
          tpsWarnThreshold +
          ")"
      )
    tickerThread = PauseableThread(this)
    tickerThread.name = tag
    tickerThread.isDaemon = true
    if (start) {
      // Do not begin ticking until the render thread is initialized
      start()
    }
  }

  override fun run() {
    val start = TimeUtils.nanoTime()
    try {
      ticking.tick()
      if (tickId % tickRareRate == 0L) {
        ticking.tickRare()
      }
    } catch (e: Throwable) {
      Main.logger().error(tag, "Failed to tick", e)
    }
    tickId++

    // Calculate ticks per second once per second
    if (start - tpsCalcStartTime >= ONE_SECOND_NANO) {
      // tps is number of times we ticked.
      //  We calculate once per second, so no dividing is required
      realTPS = tpsTick
      tpsTick = 0
      tpsCalcStartTime = start
    }
    tpsTick++
    tpsDelta = TimeUtils.nanoTime() - start
    val ms = msDelayBetweenTicks - TimeUtils.nanosToMillis(tpsDelta)
    if (ms > 0) {
      try {
        Thread.sleep(ms)
      } catch (e: InterruptedException) {
        Main.logger().error(tag, "Ticker interrupted", e)
      }
    } else if (tickId - lastTickNagged >= nagDelayTicks && tpsWarnThreshold >= realTPS) {
      lastTickNagged = tickId
      Main.logger()
        .error(
          tag,
          "Cant keep up a single tick took around " +
            TimeUtils.nanosToMillis(tpsDelta) +
            " ms, while at max it should take " +
            msDelayBetweenTicks +
            " ms " +
            nanoDelayBetweenTicks +
            " ns"
        )
    }
  }

  open fun start() {
    check(!isStarted) { "Ticker thread has already been started" }
    val threadType = currentThreadType()
    if (threadType !== ThreadType.RENDER) {
      throw CalledFromWrongThreadTypeException(
        "Tickers can only be started from the render thread, it was called from $threadType"
      )
    }
    isStarted = true
    Main.inst().scheduler.executeAsync { tickerThread.start() }
  }

  /** Stop this ticker, the tickers thread will not be called anymore  */
  open fun stop() {
    tickerThread.stopThread()
  }

  /**
   * Temporarily stops this ticker, can be resumed with [.resume]
   *
   * @see .isPaused
   * @see .resume
   */
  open fun pause() {
    tickerThread.onPause()
  }

  /**
   * Resume the ticking thread if it [.isPaused]
   *
   * @see .isPaused
   * @see .pause
   */
  open fun resume() {
    tickerThread.onResume()
  }

  val isPaused: Boolean
    /**
     * @return If this ticker thread is paused
     * @see .pause
     * @see .resume
     */
    get() = isStarted && tickerThread.isPaused

  companion object {
    const val DEFAULT_TICKS_PER_SECOND = 30L

    /** How many seconds to wait (based on the tps) between each lag message  */
    const val DEFAULT_NAG_DELAY = 3.0

    /** How low, in percent (0f..1f), the real tps must reach for the user to be warned  */
    const val TPS_LOSS_PERCENTAGE = 0.94f
    const val ONE_SECOND_NANO = 1000000000L
    const val ONE_SECOND_MILLIS = 1000L
  }
}
