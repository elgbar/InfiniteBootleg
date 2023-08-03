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

class FollowedByCameraTag : TagSavableComponent {

  companion object : TagLoadableMapper<FollowedByCameraTag>() {
    var Entity.followedByCamera by tagFor<FollowedByCameraTag>()
    var Entity.followedByCameraComponentOrNull by optionalPropertyFor(FollowedByCameraTag.mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.Tags) = with<FollowedByCameraTag>()

    override fun ProtoWorld.Entity.Tags.checkShouldLoad(): Boolean = hasFollowedByCamera()
  }

  override fun EntityKt.TagsKt.Dsl.save() {
    followedByCamera = true
  }
}
