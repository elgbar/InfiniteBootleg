package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor

data class ControlledComponent(val controlMode: ControlMode) : Component {
  companion object : Mapper<ControlledComponent>() {
    var Entity.controlled by propertyFor(mapper)
    var Entity.controlledOrNull by optionalPropertyFor(mapper)

    enum class ControlMode {
      LOCAL,
      REMOTE
    }
  }
}
