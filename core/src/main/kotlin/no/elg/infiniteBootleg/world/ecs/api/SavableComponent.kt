package no.elg.infiniteBootleg.world.ecs.api

import com.badlogic.ashley.core.Component
import no.elg.infiniteBootleg.protobuf.EntityKt

sealed interface SavableComponent<DSL> : Component {
  fun DSL.save()
}

interface TagSavableComponent : SavableComponent<EntityKt.TagsKt.Dsl>
interface EntitySavableComponent : SavableComponent<EntityKt.Dsl>
