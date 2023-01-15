package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor

sealed interface ControlledComponent : Component {
  object LocallyControlledComponent : ControlledComponent, Mapper<LocallyControlledComponent>() {
    var Entity.locallyControlled by tagFor(LocallyControlledComponent)
  }
}
