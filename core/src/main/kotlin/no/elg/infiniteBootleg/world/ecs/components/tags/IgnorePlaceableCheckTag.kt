package no.elg.infiniteBootleg.world.ecs.components.tags

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.tagFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ecs.api.TagParentLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.TagSavableComponent

class IgnorePlaceableCheckTag : TagSavableComponent {

  companion object : TagParentLoadableMapper<IgnorePlaceableCheckTag>() {
    var Entity.ignorePlaceableCheck by tagFor<IgnorePlaceableCheckTag>()
    var Entity.ignorePlaceableCheckComponentOrNull by optionalPropertyFor(IgnorePlaceableCheckTag.mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.Tags) {
      this.entity.ignorePlaceableCheck = true
    }

    override fun ProtoWorld.Entity.Tags.checkShouldLoad(): Boolean = hasIgnorePlaceableCheck()
  }

  override fun EntityKt.TagsKt.Dsl.save() {
    ignorePlaceableCheck = true
  }
}
