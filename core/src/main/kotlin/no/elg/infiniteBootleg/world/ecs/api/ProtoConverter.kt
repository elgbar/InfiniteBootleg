package no.elg.infiniteBootleg.world.ecs.api

import com.google.protobuf.Message

/**
 * Convert a type from and to a protobuf message
 */
interface ProtoConverter<COMP : Any, PROTO : Message> {

  fun PROTO.fromProto(): COMP

  fun COMP.asProto(): PROTO
}
