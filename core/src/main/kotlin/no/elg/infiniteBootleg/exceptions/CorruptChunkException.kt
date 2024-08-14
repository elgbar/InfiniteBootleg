package no.elg.infiniteBootleg.exceptions

import com.google.protobuf.TextFormat
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import kotlin.contracts.contract

class CorruptChunkException(message: String) : RuntimeException(message)

inline fun checkChunkCorrupt(protoChunk: ProtoWorld.Chunk, value: Boolean, lazyMessage: () -> Any) {
  contract {
    returns() implies value
  }
  if (!value) {
    val message = lazyMessage()
    throw CorruptChunkException(message.toString() + ". Proto chunk: ${TextFormat.printer().shortDebugString(protoChunk)}")
  }
}
