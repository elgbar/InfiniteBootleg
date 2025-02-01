package no.elg.infiniteBootleg.core.exceptions

import no.elg.infiniteBootleg.core.util.singleLinePrinter
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import kotlin.contracts.contract

class CorruptChunkException(message: String) : RuntimeException(message)

inline fun checkChunkCorrupt(protoChunk: ProtoWorld.Chunk, value: Boolean, lazyMessage: () -> Any) {
  contract {
    returns() implies value
  }
  if (!value) {
    val message = lazyMessage()
    throw CorruptChunkException(message.toString() + ". Proto chunk: ${singleLinePrinter.printToString(protoChunk)}")
  }
}
