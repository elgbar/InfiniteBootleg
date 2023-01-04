package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.ControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.ControlledComponent.Companion.controlled
import no.elg.infiniteBootleg.world.ecs.toFamily

class ControlSystem : IteratingSystem(ControlledComponent::class.toFamily(), UPDATE_PRIORITY_DEFAULT) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    if (entity.controlled.controlMode == ControlledComponent.Companion.ControlMode.LOCAL) {
//      val controls: EntityControls = getControls()
//      if (controls != null) {
//        controls.update()
//      }
    }
  }
}
