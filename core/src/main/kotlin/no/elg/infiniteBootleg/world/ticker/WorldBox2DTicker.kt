package no.elg.infiniteBootleg.world.ticker

import no.elg.infiniteBootleg.api.Ticking
import no.elg.infiniteBootleg.world.world.World

class WorldBox2DTicker(private val world: World, tick: Boolean) : Ticking {

  val ticker: Ticker = TickerImpl(this, BOX2D_TICKER_TAG_PREFIX + world.name, tick, BOX2D_TPS, Double.MAX_VALUE)

  override fun tick() {
    // tick all box2d elements
    world.worldBody.tick()
  }

  override fun tickRare() {
    world.worldBody.tickRare()
  }

  companion object {
    const val BOX2D_TPS = 60L
    const val BOX2D_TIME_STEP = 1f / BOX2D_TPS
    const val BOX2D_TICKER_TAG_PREFIX = "Box2DWorld-"
  }
}
