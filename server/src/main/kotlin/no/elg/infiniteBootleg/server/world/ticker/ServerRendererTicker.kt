package no.elg.infiniteBootleg.server.world.ticker

import no.elg.infiniteBootleg.api.Ticking
import no.elg.infiniteBootleg.server.world.ServerWorld
import no.elg.infiniteBootleg.world.ticker.Ticker
import no.elg.infiniteBootleg.world.ticker.TickerImpl

class ServerRendererTicker(private val world: ServerWorld, tick: Boolean) : Ticking {

  val ticker: Ticker = TickerImpl(this, "ServerRender-" + world.name, tick, 10, Double.MAX_VALUE)

  override fun tick() {
    world.render.update()
  }
}
