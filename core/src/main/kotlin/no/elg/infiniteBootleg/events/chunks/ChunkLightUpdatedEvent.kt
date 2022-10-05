package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.world.Chunk

data class ChunkLightUpdatedEvent(override val chunk: Chunk, val localX: Int, val localY: Int) : ChunkEvent {

  companion object {
    const val CHUNK_CENTER: Int = Chunk.CHUNK_SIZE / 2
  }
}
