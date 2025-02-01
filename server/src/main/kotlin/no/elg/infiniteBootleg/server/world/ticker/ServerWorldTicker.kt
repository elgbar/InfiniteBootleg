package no.elg.infiniteBootleg.server.world.ticker

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.world.ticker.CommonWorldTicker
import no.elg.infiniteBootleg.core.world.ticker.WorldBox2DTicker
import no.elg.infiniteBootleg.core.world.ticker.WorldTicker
import no.elg.infiniteBootleg.core.world.ticker.WorldTicker.Companion.WORLD_TICKER_TAG_PREFIX
import no.elg.infiniteBootleg.server.world.ServerWorld

/**
 * There are multiple tickers for a single world internally, but the outside world should only see a
 * single, main, ticker. When pausing, stopping, resuming the main ticker all slave tickers should
 * do the same.
 *
 *
 * Multiple tickers are needed due to some ticks will happen less frequently.
 */
class ServerWorldTicker(world: ServerWorld, tick: Boolean) : WorldTicker {

  private val ticker = CommonWorldTicker(world, tick)
  private val serverRendererTicker: ServerRendererTicker = ServerRendererTicker(world, tick)

  private val logger = KotlinLogging.logger(WORLD_TICKER_TAG_PREFIX + world.name)
  override val box2DTicker: WorldBox2DTicker get() = ticker.box2DTicker

  override fun start() {
    check(!ticker.isStarted) { "Server world has already been started" }
    ticker.start()
    serverRendererTicker.ticker.start()
    while (serverRendererTicker.ticker.tickId <= 0) {
      Thread.onSpinWait()
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
    serverRendererTicker.ticker.pause()
  }

  /**
   * Resume the ticking thread if it [isPaused]
   *
   * @see isPaused
   * @see pause
   */
  override fun resume() {
    ticker.resume()
    serverRendererTicker.ticker.resume()
  }

  override fun postRunnable(runnable: () -> Unit) = ticker.postRunnable(runnable)

  override fun stop() {
    ticker.stop()
    serverRendererTicker.ticker.stop()
  }

  override val tps: Long get() = ticker.tps
  override val secondsDelayBetweenTicks: Float get() = ticker.secondsDelayBetweenTicks
  override val tickId: Long get() = ticker.tickId
  override val realTPS: Long get() = ticker.realTPS
  override val tpsDelta: Long get() = ticker.tpsDelta
  override val isPaused: Boolean get() = ticker.isPaused
  override val isStarted: Boolean get() = ticker.isStarted
}
