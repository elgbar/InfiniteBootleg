package no.elg.infiniteBootleg.events

import no.elg.infiniteBootleg.events.api.AsyncEvent
import no.elg.infiniteBootleg.events.api.ThreadType
import no.elg.infiniteBootleg.util.ChunkColumnFeatureFlag
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.chunkToWorld
import no.elg.infiniteBootleg.world.world.World
import kotlin.math.max
import kotlin.math.min

data class ChunkColumnUpdatedEvent(
  val chunkX: ChunkCoord,
  val localX: LocalCoord,
  val newTopWorldY: WorldCoord,
  val oldTopWorldY: WorldCoord,
  val flag: ChunkColumnFeatureFlag
) : AsyncEvent(ThreadType.ASYNC) {

  /**
   * Calculate all locations from the old top coordinate to the new top coordinate, including the new changed coordinates
   */
  fun calculatedDiffColumn(): LongArray {
    val worldX = chunkX.chunkToWorld(localX)
    val minY = min(oldTopWorldY, newTopWorldY).toFloat()
    val maxY = max(oldTopWorldY, newTopWorldY).toFloat()
    val offset = maxY - minY
    return World.getLocationsAABBFromCorner(worldX.toFloat(), maxY, 0f, offset)
  }
}
