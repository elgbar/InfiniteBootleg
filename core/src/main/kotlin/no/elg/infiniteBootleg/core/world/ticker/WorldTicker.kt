package no.elg.infiniteBootleg.core.world.ticker

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.world.world.World

/**
 * The single ticker for a world. Currently only the Box2D ticker remains; this interface and its
 * [CommonWorldTicker] implementation forward everything to it.
 */
interface WorldTicker : Ticker {
  val box2DTicker: WorldBox2DTicker

  companion object {
    const val WORLD_TICKER_TAG_PREFIX = "World-"
  }
}

class CommonWorldTicker(world: World) : WorldTicker {
  override val box2DTicker: WorldBox2DTicker = WorldBox2DTicker(world)
  private val ticker: Ticker get() = box2DTicker.ticker
  private val logger = KotlinLogging.logger(WorldTicker.WORLD_TICKER_TAG_PREFIX + world.name)

  override fun start() {
    check(!ticker.isStarted) { "World has already been started" }
    ticker.start()
    while (ticker.tickId <= 0) {
      Thread.onSpinWait()
    }
    logger.info { "Started world ticker" }
  }

  override fun pause() = ticker.pause()
  override fun resume() = ticker.resume()
  override fun postRunnable(runnable: () -> Unit) = ticker.postRunnable(runnable)
  override fun dispose() = ticker.dispose()

  override val tps: Long get() = ticker.tps
  override val secondsDelayBetweenTicks: Float get() = ticker.secondsDelayBetweenTicks
  override val tickId: Long get() = ticker.tickId
  override val realTPS: Long get() = ticker.realTPS
  override val tpsDelta: Long get() = ticker.tpsDelta
  override val isPaused: Boolean get() = ticker.isPaused
  override val isStarted: Boolean get() = ticker.isStarted
  override val isDisposed: Boolean get() = ticker.isDisposed
}
