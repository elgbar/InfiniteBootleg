package no.elg.infiniteBootleg.world.ecs.api

import com.badlogic.ashley.core.Component
import com.google.protobuf.Message

interface SerializableComponent<COMP : Component, PROTO : Message> {

  fun fromProto(protoComponent: PROTO): COMP

  fun COMP.asProto(): PROTO
}
