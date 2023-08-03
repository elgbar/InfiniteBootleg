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

class GravityAffectedTag : TagSavableComponent {

  companion object : TagLoadableMapper<GravityAffectedTag>() {
    var Entity.gravityAffected by tagFor<GravityAffectedTag>()
    var Entity.gravityAffectedComponentOrNull by optionalPropertyFor(GravityAffectedTag.mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.Tags) = with<GravityAffectedTag>()

    override fun ProtoWorld.Entity.Tags.checkShouldLoad(): Boolean = hasGravityAffected()
  }

  override fun EntityKt.TagsKt.Dsl.save() {
    gravityAffected = true
  }
}
