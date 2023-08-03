package no.elg.infiniteBootleg.world.ecs.components.additional

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.input.KeyboardControls
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.ecs.api.AdditionalComponentsLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.AdditionalComponentsSavableComponent

data class LocallyControlledComponent(val keyboardControls: KeyboardControls) : AdditionalComponentsSavableComponent {

  companion object : AdditionalComponentsLoadableMapper<LocallyControlledComponent, KeyboardControls>() {
    var Entity.locallyControlledComponent by propertyFor(mapper)
    var Entity.locallyControlledComponentOrNull by optionalPropertyFor(mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.AdditionalComponents, state: KeyboardControls): LocallyControlledComponent =
      with(LocallyControlledComponent(state))

    override fun ProtoWorld.Entity.AdditionalComponents.checkShouldLoad(): Boolean = hasLocallyControlled()
  }

  override fun EntityKt.AdditionalComponentsKt.Dsl.save() {
    locallyControlled = true
  }
}
