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
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.EntityType

/**
 * A tag to mark a block which will fall (spawn a [EntityType.FALLING_BLOCK])
 *
 * @see no.elg.infiniteBootleg.core.world.ecs.system.block.FallingBlockSystem
 */
class GravityAffectedTag :
  TagSavableComponent,
  AuthoritativeOnlyComponent {

  companion object : TagLoadableMapper<GravityAffectedTag>() {
    var Entity.gravityAffected by tagFor<GravityAffectedTag>()
    var Entity.gravityAffectedComponentOrNull by optionalPropertyFor(mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.Tags) = with<GravityAffectedTag>()

    override fun ProtoWorld.Entity.Tags.checkShouldLoad(): Boolean = hasGravityAffected()
  }

  override fun EntityKt.TagsKt.Dsl.save() {
    gravityAffected = true
  }
}
