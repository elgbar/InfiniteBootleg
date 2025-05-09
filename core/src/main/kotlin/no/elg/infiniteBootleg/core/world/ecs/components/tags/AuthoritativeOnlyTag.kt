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

/**
 * This entity should not be sent to clients, since it is only used for server side logic.
 *
 * This is different from, [no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.AuthoritativeOnlyComponent] since that component is used for types of components that should not be sent to clients
 */
class AuthoritativeOnlyTag :
  TagSavableComponent,
  AuthoritativeOnlyComponent {

  companion object : TagLoadableMapper<AuthoritativeOnlyTag>() {
    var Entity.authoritativeOnly by tagFor<AuthoritativeOnlyTag>()
    val Entity.authoritativeOnlyOrNull by optionalPropertyFor(mapper)
    val Entity?.shouldSendToClients: Boolean get() = (this?.authoritativeOnlyOrNull ?: false) != true

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.Tags): AuthoritativeOnlyTag = with<AuthoritativeOnlyTag>()

    override fun ProtoWorld.Entity.Tags.checkShouldLoad(): Boolean = hasAuthoritativeOnly()
  }

  override fun EntityKt.TagsKt.Dsl.save() {
    authoritativeOnly = true
  }
}
