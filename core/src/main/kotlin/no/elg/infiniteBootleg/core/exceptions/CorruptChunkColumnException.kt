package no.elg.infiniteBootleg.core.exceptions

import no.elg.infiniteBootleg.core.util.ChunkCoord
import kotlin.contracts.contract

class CorruptChunkColumnException(message: String) : RuntimeException(message)

inline fun checkChunkColumnCorrupt(chunkX: ChunkCoord, value: Boolean, lazyMessage: () -> Any) {
  contract {
    returns() implies value
  }
  if (!value) {
    val message = lazyMessage()
    throw CorruptChunkColumnException("$message. chunkX: $chunkX")
  }
}
