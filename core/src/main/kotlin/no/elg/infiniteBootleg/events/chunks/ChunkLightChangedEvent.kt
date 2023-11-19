package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.world.chunks.Chunk

/**
 * Indicates that a chunk's light is about to be updated
 */
data class ChunkLightChangedEvent(override val chunk: Chunk, val localX: LocalCoord, val localY: LocalCoord) : ChunkEvent
