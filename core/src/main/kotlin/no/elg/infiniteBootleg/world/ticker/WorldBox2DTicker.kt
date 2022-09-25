package no.elg.infiniteBootleg.world.ticker

import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.Ticking
import no.elg.infiniteBootleg.util.Ticker
import no.elg.infiniteBootleg.world.World

class WorldBox2DTicker(private val world: World, tick: Boolean) : Ticking {

  val ticker: Ticker = Ticker(this, "Box2DWorld-" + world.name, tick, Settings.tps / BOX2D_TPS_DIVIDER, Double.MAX_VALUE)

  override fun tick() {
    // tick all box2d elements
    world.worldBody.tick()
  }

  override fun tickRare() {
    world.worldBody.tickRare()
  }

  companion object {
    const val BOX2D_TPS_DIVIDER = 2
  }
}
