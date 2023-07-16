package no.elg.infiniteBootleg.world.ecs.api

import com.badlogic.ashley.core.Component
import no.elg.infiniteBootleg.protobuf.ProtoWorld

abstract class TagParentLoadableMapper<T : Component> : ParentLoadableMapper<T, ProtoWorld.Entity.Tags>()
