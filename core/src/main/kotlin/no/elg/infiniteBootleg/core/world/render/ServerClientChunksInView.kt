package no.elg.infiniteBootleg.core.world.render

import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.util.ChunkCoord

class ServerClientChunksInView(centerX: ChunkCoord, centerY: ChunkCoord) : ChunksInView {

  var centerY = centerY
    private set
  var centerX = centerX
    private set
  override var horizontalStart: ChunkCoord
    get() = centerX - Settings.viewDistance
    set(value) {
      centerX = value + Settings.viewDistance
    }

  override var horizontalEnd: ChunkCoord
    get() = centerX + Settings.viewDistance
    set(value) {
      centerX = value - Settings.viewDistance
    }

  override var verticalStart: ChunkCoord
    get() = centerY - Settings.viewDistance
    set(value) {
      centerY = value + Settings.viewDistance
    }

  override var verticalEnd: ChunkCoord
    get() = centerY + Settings.viewDistance
    set(value) {
      centerY = value - Settings.viewDistance
    }

  fun setCenter(chunkX: ChunkCoord, chunkY: ChunkCoord) {
    centerX = chunkX
    centerY = chunkY
  }
}
