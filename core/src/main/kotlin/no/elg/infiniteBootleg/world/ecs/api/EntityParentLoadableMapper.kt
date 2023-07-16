package no.elg.infiniteBootleg.world.ecs.api

import com.badlogic.ashley.core.Component
import no.elg.infiniteBootleg.protobuf.ProtoWorld

abstract class EntityParentLoadableMapper<T : Component> : ParentLoadableMapper<T, ProtoWorld.Entity>()
