package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.events.api.ReasonedEvent
import no.elg.infiniteBootleg.util.ChunkCompactLoc

typealias ChunkTextureChangeRejectionReason = Int

/**
 * Chunk texture will not be updated
 */
data class ChunkTextureChangeRejectedEvent(override val chunkLoc: ChunkCompactLoc, val rejectReason: ChunkTextureChangeRejectionReason) : ChunkPositionEvent, ReasonedEvent {

  override val reason: String
    get() = when (rejectReason) {
      CHUNK_INVALID_REASON -> "Chunk is invalid"
      CHUNK_OUT_OF_VIEW_REASON -> "Chunk is out of view"
      CHUNK_ABOVE_TOP_BLOCK_REASON -> "Chunk is above the top block"
      else -> "Unknown reason"
    }

  companion object {

    const val CHUNK_INVALID_REASON: ChunkTextureChangeRejectionReason = 1
    const val CHUNK_OUT_OF_VIEW_REASON: ChunkTextureChangeRejectionReason = 2
    const val CHUNK_ABOVE_TOP_BLOCK_REASON: ChunkTextureChangeRejectionReason = 4
  }
}
