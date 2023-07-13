package no.elg.infiniteBootleg.world.ticker

import com.badlogic.gdx.utils.Disposable
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
  private val worldTicker =
    TickerImpl(WorldTickee(world), WORLD_TICKER_TAG_PREFIX + world.name, tick, Settings.tps, TickerImpl.DEFAULT_NAG_DELAY)
  private var serverRendererTicker: ServerRendererTicker? = null
  val box2DTicker: WorldBox2DTicker

  init {
    serverRendererTicker = (world as? ServerWorld)?.let { ServerRendererTicker(it, tick) }
    box2DTicker = WorldBox2DTicker(world, tick)
  }

  override fun start() {
    worldTicker.start()
    box2DTicker.ticker.start()
    while (worldTicker.tickId <= 0) {
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
  }

  /**
   * Temporarily stops this ticker, can be resumed with [resume]
   *
   * @see .isPaused
   * @see .resume
   */
  override fun pause() {
    worldTicker.pause()
    serverRendererTicker?.ticker?.pause()
    box2DTicker.ticker.pause()
  }

  /**
   * Resume the ticking thread if it [isPaused]
   *
   * @see .isPaused
   * @see .pause
   */
  override fun resume() {
    worldTicker.resume()
    serverRendererTicker?.ticker?.resume()
    box2DTicker.ticker.resume()
  }

  override fun postRunnable(runnable: () -> Unit) = worldTicker.postRunnable(runnable)

  override fun stop() = dispose()

  override fun dispose() {
    worldTicker.stop()
    serverRendererTicker?.ticker?.stop()
    box2DTicker.ticker.stop()
  }

  override val tps: Long get() = worldTicker.tps
  override val secondsDelayBetweenTicks: Float get() = worldTicker.secondsDelayBetweenTicks
  override val tickId: Long get() = worldTicker.tickId
  override val realTPS: Long get() = worldTicker.realTPS
  override val tpsDelta: Long get() = worldTicker.tpsDelta
  override val isPaused: Boolean get() = worldTicker.isPaused
  override val isStarted: Boolean get() = worldTicker.isStarted

  companion object {
    const val WORLD_TICKER_TAG_PREFIX = "World-"
  }
}
