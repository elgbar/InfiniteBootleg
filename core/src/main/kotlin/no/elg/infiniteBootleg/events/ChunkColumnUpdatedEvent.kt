package no.elg.infiniteBootleg.events

import no.elg.infiniteBootleg.events.api.AsyncEvent
import no.elg.infiniteBootleg.events.api.ThreadType
import no.elg.infiniteBootleg.util.ChunkColumnFeatureFlag
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.WorldCoord

data class ChunkColumnUpdatedEvent(
  val chunkX: ChunkCoord,
  val localX: LocalCoord,
  val newTopCoord: WorldCoord,
  val oldTopCoord: WorldCoord,
  val flag: ChunkColumnFeatureFlag
) : AsyncEvent(ThreadType.ASYNC)
