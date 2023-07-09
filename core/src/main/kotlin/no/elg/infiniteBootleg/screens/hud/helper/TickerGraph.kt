package no.elg.infiniteBootleg.screens.hud.helper

import no.elg.infiniteBootleg.api.Resizable
import no.elg.infiniteBootleg.screens.hud.DebugGraph
import no.elg.infiniteBootleg.world.ticker.Ticker

class TickerGraph(val ticker: Ticker, val startIndex: (width: Int) -> Int, val endIndex: (width: Int) -> Int) : Resizable {

  private var lastTickId = -1L
  private var acc = 0f
  private var index = 0

  private var startIndexValue = 0
  private var endIndexValue = 0

  val update: Boolean get() = ticker.tickId != lastTickId && !ticker.isPaused

  init {
    resize(DebugGraph.fboWidth, DebugGraph.fboHeight)
  }

  fun draw() {
    if (ticker.isPaused) {
      return
    }
    this.acc += ticker.tpsDelta.toFloat()
    val currTick = ticker.tickId

    if (currTick != lastTickId) {
      if (index >= endIndexValue) {
        index = startIndexValue
      } else {
        index += DebugGraph.COL_WIDTH
      }

      val ticks = currTick - lastTickId
      lastTickId = currTick
      val tpsDelta = ((this.acc / ticks) / 1_000_000f) + 1
      this.acc = 0f
      DebugGraph.drawColumn(tpsDelta, index)
    }
  }

  override fun resize(width: Int, height: Int) {
    index = Int.MAX_VALUE

    startIndexValue = startIndex(width)
    endIndexValue = endIndex(width)
    require(startIndexValue <= endIndexValue) {
      "start index cannot be greater or equal to end index. start index: $startIndexValue, end index: $endIndexValue"
    }
  }
}
