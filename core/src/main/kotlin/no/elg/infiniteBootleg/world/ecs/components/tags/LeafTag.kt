package no.elg.infiniteBootleg.world.ecs.components.tags

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.tagFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ecs.api.TagParentLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.TagSavableComponent

class LeafTag : TagSavableComponent {

  companion object : TagParentLoadableMapper<LeafTag>() {
    var Entity.leaf by tagFor<LeafTag>()
    var Entity.leafComponentOrNull by optionalPropertyFor(LeafTag.mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.Tags) {
      this.entity.leaf = true
    }

    override fun ProtoWorld.Entity.Tags.checkShouldLoad(): Boolean = hasFlying()
  }

  override fun EntityKt.TagsKt.Dsl.save() {
//    leaf = true
  }
}
