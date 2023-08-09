package no.elg.infiniteBootleg.world.render

import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.util.ChunkCoord

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

  fun setCenter(chunkX: ChunkCoord, chunkY: ChunkCoord) {
    centerX = chunkX
    centerY = chunkY
  }
}
