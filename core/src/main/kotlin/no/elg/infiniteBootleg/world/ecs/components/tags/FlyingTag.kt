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

class FlyingTag : TagSavableComponent, AuthoritativeOnlyComponent {

  companion object : TagLoadableMapper<FlyingTag>() {
    var Entity.flying by tagFor<FlyingTag>()
    var Entity.flyingComponentOrNull by optionalPropertyFor(FlyingTag.mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.Tags) = with<FlyingTag>()

    override fun ProtoWorld.Entity.Tags.checkShouldLoad(): Boolean = hasFlying()
  }

  override fun EntityKt.TagsKt.Dsl.save() {
    flying = true
  }
}
