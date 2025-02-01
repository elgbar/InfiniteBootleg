package no.elg.infiniteBootleg.core.world.ecs.api

import com.badlogic.ashley.core.Component
import no.elg.infiniteBootleg.core.api.HUDDebuggable
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.TagComponent
import no.elg.infiniteBootleg.protobuf.EntityKt

interface SavableComponent<DSL> : Component, HUDDebuggable {
  fun DSL.save()
}

interface TagSavableComponent : SavableComponent<EntityKt.TagsKt.Dsl>, TagComponent
interface EntitySavableComponent : SavableComponent<EntityKt.Dsl>
