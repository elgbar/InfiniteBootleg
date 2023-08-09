package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.world.chunks.Chunk

data class ChunkLightUpdatedEvent(override val chunk: Chunk, val localX: LocalCoord, val localY: LocalCoord) : ChunkEvent {

  companion object {
    const val CHUNK_CENTER: Int = Chunk.CHUNK_SIZE / 2
  }
}
