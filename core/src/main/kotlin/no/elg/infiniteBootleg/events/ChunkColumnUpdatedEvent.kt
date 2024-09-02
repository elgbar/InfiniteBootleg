package no.elg.infiniteBootleg.events

import no.elg.infiniteBootleg.events.api.AsyncEvent
import no.elg.infiniteBootleg.events.api.ReasonedEvent
import no.elg.infiniteBootleg.events.api.ThreadType
import no.elg.infiniteBootleg.util.ChunkColumnFeatureFlag
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.WorldCompactLocArray
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.chunkToWorld
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.featureFlagToString
import no.elg.infiniteBootleg.world.world.World
import kotlin.math.max
import kotlin.math.min

data class ChunkColumnUpdatedEvent(
  val chunkX: ChunkCoord,
  val localX: LocalCoord,
  val newTopWorldY: WorldCoord,
  val oldTopWorldY: WorldCoord,
  val flag: ChunkColumnFeatureFlag
) : ReasonedEvent, AsyncEvent(ThreadType.ASYNC) {

  /**
   * All world locations from the old top coordinate to the new top coordinate, including the new changed coordinates
   */
  val calculatedDiffColumn: WorldCompactLocArray by lazy {
    val worldX = chunkX.chunkToWorld(localX)
    val minY = min(oldTopWorldY, newTopWorldY).toFloat()
    val maxY = max(oldTopWorldY, newTopWorldY).toFloat()
    val offset = maxY - minY
    World.getLocationsAABBFromCorner(worldX.toFloat(), maxY, 0f, offset)
  }
  override val reason: String
    get() = "Chunk column updated with flag ${featureFlagToString(flag)}"
}
