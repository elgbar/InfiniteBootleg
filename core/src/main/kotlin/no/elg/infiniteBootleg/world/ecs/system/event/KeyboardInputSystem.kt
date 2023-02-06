package no.elg.infiniteBootleg.world.ecs.system.event

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.input.KeyboardControls
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent
import no.elg.infiniteBootleg.world.ecs.controlledEntityWithInputEventFamily

object KeyboardInputSystem : EventSystem<InputEvent>(controlledEntityWithInputEventFamily, UPDATE_PRIORITY_DEFAULT, InputEvent::class) {

  override fun handleEvent(entity: Entity, deltaTime: Float) {
    handleInputEvent(entity, InputEvent.KeyDownEvent.mapper) {
      KeyboardControls.keyDown(entity, it.keycode)
    }
    handleInputEvent(entity, InputEvent.TouchDownEvent.mapper) {
      KeyboardControls.touchDown(entity, it.button)
    }
  }
}
