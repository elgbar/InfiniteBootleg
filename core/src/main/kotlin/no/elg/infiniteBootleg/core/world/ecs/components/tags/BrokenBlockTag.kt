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
 * Mark this entity as having the attached block as broken. Meaning the block should be removed the next tick
 *
 * @see no.elg.infiniteBootleg.core.world.ecs.system.block.BrokenBlockCleanupSystem
 */
class BrokenBlockTag :
  TagSavableComponent,
  AuthoritativeOnlyComponent {

  companion object : TagLoadableMapper<BrokenBlockTag>() {
    var Entity.brokenBlock by tagFor<BrokenBlockTag>()
    var Entity.brokenBlockComponentOrNull by optionalPropertyFor(mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.Tags) = with<BrokenBlockTag>()

    override fun ProtoWorld.Entity.Tags.checkShouldLoad(): Boolean = hasBrokenBlock()
  }

  override fun EntityKt.TagsKt.Dsl.save() {
    brokenBlock = true
  }
}
