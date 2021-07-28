package no.elg.infiniteBootleg.world.ticker

import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.Ticking
import no.elg.infiniteBootleg.util.Ticker
import no.elg.infiniteBootleg.world.World
import kotlin.system.measureTimeMillis

class WorldBox2DTicker internal constructor(private val world: World, tick: Boolean) : Ticking {
  val ticker: Ticker
  override fun tick() {
    val time = measureTimeMillis {
      //tick all box2d elements
      world.worldBody.tick()
    }
  }

  init {
    ticker = Ticker(this, "Box2DWorldLight-" + world.name, tick, Settings.tps, Double.MAX_VALUE)
  }
}
