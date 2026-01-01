package no.elg.infiniteBootleg.core.world.ticker

import com.badlogic.gdx.utils.PauseableThread
import com.badlogic.gdx.utils.TimeUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.api.Ticking
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.util.FailureWatchdog

/**
 * A helper class that calls a [Ticking]'s [Ticking.tick] and [Ticking.tickRare] method periodically. By default, it will call it every [msDelayBetweenTicks].
 *
 *
 * The ticker will not update the if [com.badlogic.gdx.Graphics.getFrameId] is the same as it was last
 * tick (as can be caused by fps lag), and will warn when too many ticks have been skipped.
 *
 * @param ticking The ticker to tick
 * @param name Name of the ticker thread
 * @param tps Ticks per seconds, must be a strictly positive number
 * @param nagDelay Minimum seconds between each nag message, If less than or equal to zero there will be no delay (note that this will be a lot of spam!)
 *
 * @author Elg
 */
class TickerImpl(private val ticking: Ticking, name: String, tps: Long, nagDelay: Double) :
  Ticker,
  Runnable {
  /**
   * How many ticks between each rare update. Currently, each rare tick is the same as one second
   */
  private val tickRareRate: Long
  private val tickerThread: PauseableThread

  private val tag: String = "$name ticker"
  private val logger = KotlinLogging.logger(tag)

  override val tps: Long
  override val secondsDelayBetweenTicks: Float
  private val msDelayBetweenTicks: Long
  private val nanoDelayBetweenTicks: Long
  private val nagDelayTicks: Long

  /** How low the tps must reach before displaying a "can't keep up" warning  */
  private val tpsWarnThreshold: Long

  override var tickId: Long = 0
    private set

  override var realTPS: Long = -1
    private set

  /** At what tick did we start the tps calculation  */
  private var tpsCalcStartTime: Long = 0

  /** Ticks accumulated during calculating tps  */
  private var tpsTick: Long = 0

  override var tpsDelta: Long = 0
    private set

  /** How long ago we last showed a "can't keep up" warning  */
  private var lastTickNagged: Long = 0

  @Volatile
  override var isStarted = false
    private set

  init {
    require(tps > 0) { "TPS must be strictly positive! Was given $tps" }
    this.tps = tps
    msDelayBetweenTicks = ONE_SECOND_MILLIS / tps
    nanoDelayBetweenTicks = ONE_SECOND_NANO / tps
    secondsDelayBetweenTicks = 1f / tps
    tickRareRate = this.tps
    // Round down ok as realTPS is a long
    tpsWarnThreshold = (TPS_LOSS_PERCENTAGE * tps).toLong()
    nagDelayTicks = (tps * nagDelay).coerceAtLeast(0.0).toLong()
    logger.debug { "Starting ticking thread for '$name' with tps = $tps (warn when tps <= $tpsWarnThreshold)" }
    tickerThread = PauseableThread(this)
    tickerThread.name = tag
    tickerThread.isDaemon = true
  }

  private val postRunnableHandler: PostRunnableHandler = PostRunnableHandler()

  override fun postRunnable(runnable: () -> Unit) = postRunnableHandler.postRunnable(runnable)

  private val watchdog = FailureWatchdog("tick '$name' ticker")

  override fun run() {
    val start = TimeUtils.nanoTime()
    watchdog.watch(this) {
      ticking.tick()
      if (tickId % tickRareRate == 0L) {
        ticking.tickRare()
      }
      postRunnableHandler.executeRunnables()
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
        logger.debug(e) { "$tag interrupted, disposed? $isDisposed, paused? $isPaused" }
      }
    } else if (tickId - lastTickNagged >= nagDelayTicks && tpsWarnThreshold >= realTPS) {
      lastTickNagged = tickId
      logger.error {
        "Cant keep up a single tick took around ${TimeUtils.nanosToMillis(tpsDelta)} ms, while at max it should take $msDelayBetweenTicks ms $nanoDelayBetweenTicks ns"
      }
    }
  }

  override fun start() {
    check(!isStarted) { "Ticker thread has already been started" }
    ThreadType.RENDER.requireCorrectThreadType { "Tickers can only be started from the render thread" }
    isStarted = true
    tickerThread.start()
  }

  /**
   * Temporarily stops this ticker, can be resumed with [resume]
   *
   * @see isPaused
   * @see resume
   */
  override fun pause() {
    tickerThread.onPause()
  }

  /**
   * Resume the ticking thread if it [isPaused]
   *
   * @see isPaused
   * @see pause
   */
  override fun resume() {
    tickerThread.onResume()
  }

  override val isPaused: Boolean
    get() = isStarted && tickerThread.isPaused
  override var isDisposed: Boolean = false
    private set

  override fun dispose() {
    isDisposed = true
    tickerThread.stopThread()
  }

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
