package no.elg.infiniteBootleg.world.render

import no.elg.infiniteBootleg.Settings

class ServerClientChunksInView(private var centerX: Int, private var centerY: Int) : ChunksInView {

  override var horizontalStart: Int
    get() = centerX - Settings.viewDistance
    set(value) {
      centerX = value + Settings.viewDistance
    }

  override var horizontalEnd: Int
    get() = centerX + Settings.viewDistance
    set(value) {
      centerX = value - Settings.viewDistance
    }

  override var verticalStart: Int
    get() = centerY - Settings.viewDistance
    set(value) {
      centerY = value + Settings.viewDistance
    }

  override var verticalEnd: Int
    get() = centerY + Settings.viewDistance
    set(value) {
      centerY = value - Settings.viewDistance
    }

  fun setCenter(chunkX: Int, chunkY: Int) {
    centerX = chunkX
    centerY = chunkY
  }
}
