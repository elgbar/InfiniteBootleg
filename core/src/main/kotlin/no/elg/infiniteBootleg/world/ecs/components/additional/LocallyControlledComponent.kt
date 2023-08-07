package no.elg.infiniteBootleg.world.ecs.components.additional

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.input.KeyboardControls
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.locallyControlled
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.api.LoadableMapper

data class LocallyControlledComponent(val keyboardControls: KeyboardControls) : EntitySavableComponent {

  companion object : LoadableMapper<LocallyControlledComponent, ProtoWorld.Entity, KeyboardControls>() {
    var Entity.locallyControlledComponent by propertyFor(mapper)
    var Entity.locallyControlledComponentOrNull by optionalPropertyFor(mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity, state: KeyboardControls): LocallyControlledComponent =
      with(
        LocallyControlledComponent(
          state.apply {
            val locallyControlled = protoEntity.locallyControlled
            instantBreak = locallyControlled.instantBreak
            brushSize = locallyControlled.brushRadius
            interactRadius = locallyControlled.interactRadius
          }
        )
      )

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasLocallyControlled()
  }

  override fun EntityKt.Dsl.save() {
    locallyControlled = locallyControlled {
      instantBreak = keyboardControls.instantBreak
      brushRadius = keyboardControls.brushSize
      interactRadius = keyboardControls.interactRadius
    }
  }
}
