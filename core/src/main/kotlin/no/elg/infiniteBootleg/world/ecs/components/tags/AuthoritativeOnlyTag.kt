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

/**
 * This entity should not be sent to clients, since it is only used for server side logic
 */
class AuthoritativeOnlyTag : TagSavableComponent {

  companion object : TagLoadableMapper<AuthoritativeOnlyTag>() {
    var Entity.authoritativeOnly by tagFor<AuthoritativeOnlyTag>()
    val Entity.authoritativeOnlyOrNull by optionalPropertyFor(AuthoritativeOnlyTag.mapper)
    val Entity?.shouldSendToClients: Boolean get() = (this?.authoritativeOnlyOrNull ?: false) != true

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.Tags): AuthoritativeOnlyTag = with<AuthoritativeOnlyTag>()

    override fun ProtoWorld.Entity.Tags.checkShouldLoad(): Boolean = hasLeafDecay()
  }

  override fun EntityKt.TagsKt.Dsl.save() {
    authoritativeOnly = true
  }
}