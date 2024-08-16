package no.elg.infiniteBootleg.world.render

import no.elg.infiniteBootleg.util.ChunkCoord

/** @author Elg
 */
data class ClientChunksInView(
  override var horizontalStart: ChunkCoord = 0,
  override var horizontalEnd: ChunkCoord = 0,
  override var verticalStart: ChunkCoord = 0,
  override var verticalEnd: ChunkCoord = 0
) : ChunksInView
