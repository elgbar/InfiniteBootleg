package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.input.KeyboardControls

data class LocallyControlledComponent(val keyboardControls: KeyboardControls) : Component {

  companion object : Mapper<LocallyControlledComponent>() {
    var Entity.locallyControlled by propertyFor(mapper)
    var Entity.locallyControlledOrNull by optionalPropertyFor(mapper)
  }
}
