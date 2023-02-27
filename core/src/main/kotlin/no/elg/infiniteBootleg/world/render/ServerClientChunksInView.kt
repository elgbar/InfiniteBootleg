package no.elg.infiniteBootleg.world.render

import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.world.Location

class ServerClientChunksInView(initialChunk: Location) : ChunksInView {

  var centerX = initialChunk.x
  var centerY = initialChunk.y

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
