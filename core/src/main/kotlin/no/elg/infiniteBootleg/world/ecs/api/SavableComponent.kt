package no.elg.infiniteBootleg.world.ecs.api

import com.badlogic.ashley.core.Component
import no.elg.infiniteBootleg.protobuf.EntityKt

interface SavableComponent<DSL> : Component {
  fun DSL.save()
}

interface ProtoConvertable<DSL> : Component {
  fun toProtobuf(): DSL
}

interface TagSavableComponent : SavableComponent<EntityKt.TagsKt.Dsl>
interface EntitySavableComponent : SavableComponent<EntityKt.Dsl>
