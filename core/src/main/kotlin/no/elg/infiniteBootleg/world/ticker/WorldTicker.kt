package no.elg.infiniteBootleg.world.ticker

import com.badlogic.gdx.utils.Disposable
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.world.world.ServerWorld
import no.elg.infiniteBootleg.world.world.World

/**
 * There are multiple tickers for a single world internally, but the outside world should only see a
 * single, main, ticker. When pausing, stopping, resuming the main ticker all slave tickers should
 * do the same.
 *
 *
 * Multiple tickers are needed due to some ticks will happen less frequently.
 */
class WorldTicker(world: World, tick: Boolean) : Ticker, Disposable {
  private val ticker =
    TickerImpl(WorldTickee(world), WORLD_TICKER_TAG_PREFIX + world.name, tick, Settings.tps, TickerImpl.DEFAULT_NAG_DELAY)
  private var serverRendererTicker: ServerRendererTicker? = null
  val box2DTicker: WorldBox2DTicker
  private val logger = KotlinLogging.logger(WORLD_TICKER_TAG_PREFIX + world.name)

  init {
    serverRendererTicker = (world as? ServerWorld)?.let { ServerRendererTicker(it, tick) }
    box2DTicker = WorldBox2DTicker(world, tick)
  }

  override fun start() {
    check(!ticker.isStarted) { "World has already been started" }
    ticker.start()
    box2DTicker.ticker.start()
    while (ticker.tickId <= 0) {
      Thread.onSpinWait()
    }
    while (box2DTicker.ticker.tickId <= 0) {
      Thread.onSpinWait()
    }
    serverRendererTicker?.also {
      it.ticker.start()
      while (it.ticker.tickId <= 0) {
        Thread.onSpinWait()
      }
    }
    logger.info { "Started world ticker" }
  }

  /**
   * Temporarily stops this ticker, can be resumed with [resume]
   *
   * @see isPaused
   * @see resume
   */
  override fun pause() {
    ticker.pause()
    serverRendererTicker?.ticker?.pause()
    box2DTicker.ticker.pause()
  }

  /**
   * Resume the ticking thread if it [isPaused]
   *
   * @see isPaused
   * @see pause
   */
  override fun resume() {
    ticker.resume()
    serverRendererTicker?.ticker?.resume()
    box2DTicker.ticker.resume()
  }

  override fun postRunnable(runnable: () -> Unit) = ticker.postRunnable(runnable)

  override fun stop() = dispose()

  override fun dispose() {
    ticker.stop()
    serverRendererTicker?.ticker?.stop()
    box2DTicker.ticker.stop()
  }

  override val tps: Long get() = ticker.tps
  override val secondsDelayBetweenTicks: Float get() = ticker.secondsDelayBetweenTicks
  override val tickId: Long get() = ticker.tickId
  override val realTPS: Long get() = ticker.realTPS
  override val tpsDelta: Long get() = ticker.tpsDelta
  override val isPaused: Boolean get() = ticker.isPaused
  override val isStarted: Boolean get() = ticker.isStarted

  companion object {
    const val WORLD_TICKER_TAG_PREFIX = "World-"
  }
}
