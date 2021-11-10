package no.elg.infiniteBootleg.world.ticker

import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.Ticking
import no.elg.infiniteBootleg.util.Ticker
import no.elg.infiniteBootleg.world.World

class WorldBox2DTicker(private val world: World, tick: Boolean) : Ticking {

  val ticker: Ticker = Ticker(this, "Box2DWorldLight-" + world.name, tick, Settings.tps, Double.MAX_VALUE)

  override fun tick() {
    // tick all box2d elements
    world.worldBody.tick()
  }

  override fun tickRare() {
    world.worldBody.tickRare()
  }
}
