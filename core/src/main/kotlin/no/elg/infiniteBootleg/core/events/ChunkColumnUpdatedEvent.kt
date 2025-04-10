package no.elg.infiniteBootleg.core.events

import no.elg.infiniteBootleg.core.events.api.AsyncEvent
import no.elg.infiniteBootleg.core.events.api.ReasonedEvent
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.util.ChunkColumnFeatureFlag
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.WorldCompactLocArray
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.chunkToWorld
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumn
import no.elg.infiniteBootleg.core.world.world.World
import kotlin.math.max
import kotlin.math.min

data class ChunkColumnUpdatedEvent(val chunkX: ChunkCoord, val localX: LocalCoord, val newTopWorldY: WorldCoord, val oldTopWorldY: WorldCoord, val flag: ChunkColumnFeatureFlag) :
  AsyncEvent(ThreadType.ASYNC),
  ReasonedEvent {

  /**
   * All world locations from the old top coordinate to the new top coordinate, including the new changed coordinates
   */
  val calculatedDiffColumn: WorldCompactLocArray by lazy {
    val worldX = chunkX.chunkToWorld(localX)
    val minY = min(oldTopWorldY, newTopWorldY).toFloat()
    val maxY = max(oldTopWorldY, newTopWorldY).toFloat()
    val offset = maxY - minY
    World.Companion.getLocationsAABBFromCorner(worldX.toFloat(), maxY, 0f, offset)
  }
  override val reason: String
    get() = "Chunk column updated with flag ${ChunkColumn.Companion.FeatureFlag.featureFlagToString(flag)}"
}
