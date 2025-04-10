package no.elg.infiniteBootleg.core.world.ecs.components.tags

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.tagFor
import ktx.ashley.with
import no.elg.infiniteBootleg.core.world.ecs.api.TagLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.TagSavableComponent
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.AuthoritativeOnlyComponent
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld

class FollowedByCameraTag :
  TagSavableComponent,
  AuthoritativeOnlyComponent {

  companion object : TagLoadableMapper<FollowedByCameraTag>() {
    var Entity.followedByCamera by tagFor<FollowedByCameraTag>()
    var Entity.followedByCameraComponentOrNull by optionalPropertyFor(mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.Tags) = with<FollowedByCameraTag>()

    override fun ProtoWorld.Entity.Tags.checkShouldLoad(): Boolean = hasFollowedByCamera()
  }

  override fun EntityKt.TagsKt.Dsl.save() {
    followedByCamera = true
  }
}
