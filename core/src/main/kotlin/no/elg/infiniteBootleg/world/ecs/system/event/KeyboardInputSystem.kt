package no.elg.infiniteBootleg.world.ecs.system.event

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.input.KeyboardControls
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEventQueue
import no.elg.infiniteBootleg.world.ecs.controlledEntityWithInputEventFamily

object KeyboardInputSystem : EventSystem<InputEvent, InputEventQueue>(controlledEntityWithInputEventFamily, UPDATE_PRIORITY_DEFAULT, InputEvent::class, InputEventQueue.mapper) {

  override fun handleEvent(entity: Entity, deltaTime: Float, event: InputEvent) {
    when (event) {
      is InputEvent.KeyDownEvent -> KeyboardControls.keyDown(entity, event.keycode)
      is InputEvent.TouchDownEvent -> KeyboardControls.touchDown(entity, event.button)
      is InputEvent.KeyTypedEvent -> Unit
      is InputEvent.KeyUpEvent -> Unit
      is InputEvent.MouseMovedEvent -> Unit
      is InputEvent.ScrolledEvent -> Unit
      is InputEvent.TouchDraggedEvent -> Unit
      is InputEvent.TouchUpEvent -> Unit
    }
  }
}
