package no.elg.infiniteBootleg.core.world.ecs.components.tags

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.tagFor
import ktx.ashley.with
import no.elg.infiniteBootleg.core.world.ecs.api.TagLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.TagSavableComponent
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.AuthoritativeOnlyComponent
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2dOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.tags.FlyingTag.Companion.flying
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld

class FlyingTag :
  TagSavableComponent,
  AuthoritativeOnlyComponent {

  companion object : TagLoadableMapper<FlyingTag>() {
    var Entity.flying by tagFor<FlyingTag>()

    /**
     * Make the player fly if [flying] is true, otherwise disable aka gravity
     */
    fun Entity.ensureFlyingStatus() {
      val box2d = this.box2dOrNull
      if (box2d != null) {
        // Disable gravity
        if (flying) {
          box2d.disableGravity()
        } else {
          box2d.enableGravity()
        }
      }
    }

    var Entity.flyingComponentOrNull by optionalPropertyFor(mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.Tags) = with<FlyingTag>()

    override fun ProtoWorld.Entity.Tags.checkShouldLoad(): Boolean = hasFlying()
  }

  override fun EntityKt.TagsKt.Dsl.save() {
    flying = true
  }
}
