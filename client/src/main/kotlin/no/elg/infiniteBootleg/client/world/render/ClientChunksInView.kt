package no.elg.infiniteBootleg.client.world.render

import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.world.render.ChunksInView

/** @author Elg */
data class ClientChunksInView(
  override var horizontalStart: ChunkCoord = 0,
  override var horizontalEnd: ChunkCoord = 0,
  override var verticalStart: ChunkCoord = 0,
  override var verticalEnd: ChunkCoord = 0
) : ChunksInView
