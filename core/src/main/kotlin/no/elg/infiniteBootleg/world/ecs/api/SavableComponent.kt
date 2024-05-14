package no.elg.infiniteBootleg.world.ecs.api

import com.badlogic.ashley.core.Component
import no.elg.infiniteBootleg.api.HUDDebuggable
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.world.ecs.api.restriction.component.TagComponent

interface SavableComponent<DSL> : Component, HUDDebuggable {
  fun DSL.save()
}

interface TagSavableComponent : SavableComponent<EntityKt.TagsKt.Dsl>, TagComponent
interface EntitySavableComponent : SavableComponent<EntityKt.Dsl>
