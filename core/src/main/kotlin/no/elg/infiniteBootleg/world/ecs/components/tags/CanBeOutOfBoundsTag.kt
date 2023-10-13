package no.elg.infiniteBootleg.world.ecs.components.tags

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.tagFor
import ktx.ashley.with
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ecs.api.TagLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.TagSavableComponent
import no.elg.infiniteBootleg.world.ecs.api.restriction.AuthoritativeOnlyComponent

class CanBeOutOfBoundsTag : TagSavableComponent, AuthoritativeOnlyComponent {

  companion object : TagLoadableMapper<CanBeOutOfBoundsTag>() {
    var Entity.canBeOutOfBounds by tagFor<CanBeOutOfBoundsTag>()
    var Entity.canBeOutOfBoundsComponentOrNull by optionalPropertyFor(CanBeOutOfBoundsTag.mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.Tags) = with<CanBeOutOfBoundsTag>()

    override fun ProtoWorld.Entity.Tags.checkShouldLoad(): Boolean = hasCanBeOutOfBounds()
  }

  override fun EntityKt.TagsKt.Dsl.save() {
    canBeOutOfBounds = true
  }
}
