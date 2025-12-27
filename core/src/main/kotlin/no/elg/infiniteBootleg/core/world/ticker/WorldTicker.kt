package no.elg.infiniteBootleg.core.world.ticker

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.world.world.World

/**
 * There are multiple tickers for a single world internally, but the outside world should only see a
 * single, main, ticker. When pausing, stopping, resuming the main ticker all slave tickers should
 * do the same.
 *
 *
 * Multiple tickers are needed due to some ticks will happen less frequently.
 */
interface WorldTicker : Ticker {
  val box2DTicker: WorldBox2DTicker

  companion object {
    const val WORLD_TICKER_TAG_PREFIX = "World-"
  }
}

class CommonWorldTicker(world: World, tick: Boolean) : WorldTicker {
  private val ticker = TickerImpl(WorldTickee(world), WorldTicker.WORLD_TICKER_TAG_PREFIX + world.name, tick, Settings.tps, TickerImpl.DEFAULT_NAG_DELAY)
  override val box2DTicker: WorldBox2DTicker = WorldBox2DTicker(world, tick)
  private val logger = KotlinLogging.logger(WorldTicker.WORLD_TICKER_TAG_PREFIX + world.name)

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
    logger.info { "Started world tickers" }
  }

  /**
   * Temporarily stops this ticker, can be resumed with [resume]
   *
   * @see isPaused
   * @see resume
   */
  override fun pause() {
    ticker.pause()
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
    box2DTicker.ticker.resume()
  }

  override fun postRunnable(runnable: () -> Unit) = ticker.postRunnable(runnable)

  override fun dispose() {
    ticker.dispose()
    box2DTicker.ticker.dispose()
  }

  override val tps: Long get() = ticker.tps
  override val secondsDelayBetweenTicks: Float get() = ticker.secondsDelayBetweenTicks
  override val tickId: Long get() = ticker.tickId
  override val realTPS: Long get() = ticker.realTPS
  override val tpsDelta: Long get() = ticker.tpsDelta
  override val isPaused: Boolean get() = ticker.isPaused
  override val isStarted: Boolean get() = ticker.isStarted
  override val isDisposed: Boolean get() = ticker.isDisposed
}
