package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.input.KeyboardControls
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent
import no.elg.infiniteBootleg.world.ecs.controlledEntityWithEventFamily

object KeyboardInputSystem : IteratingSystem(controlledEntityWithEventFamily, UPDATE_PRIORITY_DEFAULT) {

  private val sealedSubclasses = InputEvent::class.sealedSubclasses

  override fun processEntity(entity: Entity, deltaTime: Float) {
    handleInputEvent(entity, InputEvent.KeyDownEvent.mapper) {
      KeyboardControls.keyDown(entity, it.keycode)
    }
    handleInputEvent(entity, InputEvent.TouchDownEvent.mapper) {
      KeyboardControls.touchDown(entity, it.button)
    }
//    handleInputEvent(entity, InputEvent.TouchUpEvent.mapper) {
//      KeyboardControls.touchUp(entity, it)
//    }
//    handleInputEvent(entity, InputEvent.TouchDraggedEvent.mapper) {
//      KeyboardControls.touchDragged(entity, it)
//    }

    for (subclass in sealedSubclasses) {
      entity.remove(subclass.java)
    }
  }

  private inline fun <reified T : Component> handleInputEvent(entity: Entity, mapper: ComponentMapper<T>, action: (T) -> Unit) {
    mapper.get(entity)?.also(action)
  }
}
