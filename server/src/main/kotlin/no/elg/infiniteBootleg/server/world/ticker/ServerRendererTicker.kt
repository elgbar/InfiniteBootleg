package no.elg.infiniteBootleg.server.world.ticker

import no.elg.infiniteBootleg.core.api.Ticking
import no.elg.infiniteBootleg.core.world.ticker.Ticker
import no.elg.infiniteBootleg.core.world.ticker.TickerImpl
import no.elg.infiniteBootleg.server.world.ServerWorld

class ServerRendererTicker(private val world: ServerWorld) : Ticking {

  val ticker: Ticker = TickerImpl(this, "ServerRender-" + world.name, 10, Double.MAX_VALUE)

  override fun tick() {
    world.render.update()
  }
}
