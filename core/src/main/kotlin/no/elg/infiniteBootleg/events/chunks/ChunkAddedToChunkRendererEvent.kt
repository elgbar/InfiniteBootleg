package no.elg.infiniteBootleg.events.chunks

import no.elg.infiniteBootleg.util.ChunkCompactLoc

class ChunkAddedToChunkRendererEvent(override val chunkLoc: ChunkCompactLoc, val prioritized: Boolean) : ChunkPositionEvent
