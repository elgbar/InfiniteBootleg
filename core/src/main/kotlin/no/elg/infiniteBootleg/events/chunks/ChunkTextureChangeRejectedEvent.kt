package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.util.ChunkCompactLoc

typealias ChunkTextureChangeRejectionReason = Int

/**
 * Chunk texture will not be updated
 */
class ChunkTextureChangeRejectedEvent(override val chunkLoc: ChunkCompactLoc, val reason: ChunkTextureChangeRejectionReason) : ChunkPositionEvent {

  companion object {

    const val CHUNK_INVALID_REASON: ChunkTextureChangeRejectionReason = 1
    const val CHUNK_OUT_OF_VIEW_REASON: ChunkTextureChangeRejectionReason = 2
    const val CHUNK_ABOVE_TOP_BLOCK_REASON: ChunkTextureChangeRejectionReason = 4
  }
}
