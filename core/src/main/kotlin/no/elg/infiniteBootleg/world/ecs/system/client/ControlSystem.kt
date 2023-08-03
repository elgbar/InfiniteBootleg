package no.elg.infiniteBootleg.world.ecs.system.client

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_EARLY
import no.elg.infiniteBootleg.world.ecs.components.additional.LocallyControlledComponent.Companion.locallyControlledComponent
import no.elg.infiniteBootleg.world.ecs.controlledEntityFamily

object ControlSystem : IteratingSystem(controlledEntityFamily, UPDATE_PRIORITY_EARLY) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    entity.locallyControlledComponent.keyboardControls.update(entity)
  }
}
