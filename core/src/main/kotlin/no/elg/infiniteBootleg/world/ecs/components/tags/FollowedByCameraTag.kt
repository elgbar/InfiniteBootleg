package no.elg.infiniteBootleg.world.ecs.components.tags

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.tagFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ecs.api.TagParentLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.TagSavableComponent

class FollowedByCameraTag : TagSavableComponent {

  companion object : TagParentLoadableMapper<FollowedByCameraTag>() {
    var Entity.followedByCamera by tagFor<FollowedByCameraTag>()
    var Entity.followedByCameraComponentOrNull by optionalPropertyFor(FollowedByCameraTag.mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.Tags) {
      this.entity.followedByCamera = true
    }

    override fun ProtoWorld.Entity.Tags.checkShouldLoad(): Boolean = hasFollowedByCamera()
  }

  override fun EntityKt.TagsKt.Dsl.save() {
    followedByCamera = true
  }
}
