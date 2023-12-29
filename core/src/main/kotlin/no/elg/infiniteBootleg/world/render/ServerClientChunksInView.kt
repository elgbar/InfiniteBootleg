package no.elg.infiniteBootleg.world.render

import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.util.ChunkCoord

class ServerClientChunksInView(private var centerX: ChunkCoord, private var centerY: ChunkCoord) : ChunksInView {

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
