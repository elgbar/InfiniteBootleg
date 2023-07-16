package no.elg.infiniteBootleg.world.ecs.components.tags

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.tagFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ecs.api.TagParentLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.TagSavableComponent

class GravityAffectedTag : TagSavableComponent {

  companion object : TagParentLoadableMapper<GravityAffectedTag>() {
    var Entity.gravityAffected by tagFor<GravityAffectedTag>()
    var Entity.gravityAffectedComponentOrNull by optionalPropertyFor(GravityAffectedTag.mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.Tags) {
      this.entity.gravityAffected = true
    }

    override fun ProtoWorld.Entity.Tags.checkShouldLoad(): Boolean = hasGravityAffected()
  }

  override fun EntityKt.TagsKt.Dsl.save() {
    gravityAffected = true
  }
}
