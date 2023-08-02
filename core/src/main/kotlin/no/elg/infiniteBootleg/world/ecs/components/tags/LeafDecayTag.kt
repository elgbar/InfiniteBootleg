package no.elg.infiniteBootleg.world.ecs.components.tags

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.tagFor
import ktx.ashley.with
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ecs.api.TagParentLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.TagSavableComponent

class LeafDecayTag : TagSavableComponent {

  companion object : TagParentLoadableMapper<LeafDecayTag>() {
    var Entity.leafDecay by tagFor<LeafDecayTag>()
    var Entity.leafDecayComponentOrNull by optionalPropertyFor(LeafDecayTag.mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.Tags): LeafDecayTag = with<LeafDecayTag>()

    override fun ProtoWorld.Entity.Tags.checkShouldLoad(): Boolean = hasLeafDecay()
  }

  override fun EntityKt.TagsKt.Dsl.save() {
    leafDecay = true
  }
}
