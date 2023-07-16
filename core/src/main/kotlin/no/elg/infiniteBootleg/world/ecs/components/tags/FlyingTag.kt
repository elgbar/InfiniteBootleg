package no.elg.infiniteBootleg.world.ecs.components.tags

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.tagFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ecs.api.TagParentLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.TagSavableComponent

class FlyingTag : TagSavableComponent {

  companion object : TagParentLoadableMapper<FlyingTag>() {
    var Entity.flying by tagFor<FlyingTag>()
    var Entity.flyingComponentOrNull by optionalPropertyFor(FlyingTag.mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.Tags) {
      this.entity.flying = true
    }

    override fun ProtoWorld.Entity.Tags.checkShouldLoad(): Boolean = hasFlying()
  }

  override fun EntityKt.TagsKt.Dsl.save() {
    flying = true
  }
}
