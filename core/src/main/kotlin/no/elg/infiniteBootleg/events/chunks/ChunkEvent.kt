package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.events.api.Event
import no.elg.infiniteBootleg.world.chunks.Chunk

interface ChunkEvent : Event {
  val chunk: Chunk
}
