package no.elg.infiniteBootleg.world.ticker

import no.elg.infiniteBootleg.Ticking
import no.elg.infiniteBootleg.util.Ticker
import no.elg.infiniteBootleg.world.ServerWorld

class ServerRendererTicker(private val world: ServerWorld, tick: Boolean) : Ticking {

  val ticker: Ticker = Ticker(this, "ServerRender-" + world.name, tick, 1, Double.MAX_VALUE)

  override fun tick() {
    world.render.update()
  }
}
