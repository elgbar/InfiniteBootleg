package no.elg.infiniteBootleg.world.ecs.components.events

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor

sealed interface InputEvent : ECSEvent {
  data class KeyDownEvent(val keycode: Int) : InputEvent {
    companion object : Mapper<KeyDownEvent>() {
      var Entity.keyDownEventOrNull by optionalPropertyFor(mapper)
    }
  }

  data class KeyUpEvent(val keycode: Int) : InputEvent {
    companion object : Mapper<KeyUpEvent>() {
      var Entity.keyUpEventOrNull by optionalPropertyFor(mapper)
    }
  }

  data class KeyTypedEvent(val char: Char) : InputEvent {
    companion object : Mapper<KeyTypedEvent>() {
      var Entity.keyTypedEventOrNull by optionalPropertyFor(mapper)
    }
  }

  data class TouchDownEvent(val screenX: Int, val screenY: Int, val pointer: Int, val button: Int) : InputEvent {
    companion object : Mapper<TouchDownEvent>() {
      var Entity.touchDownEventOrNull by optionalPropertyFor(mapper)
    }
  }

  data class TouchUpEvent(val screenX: Int, val screenY: Int, val pointer: Int, val button: Int) : InputEvent {
    companion object : Mapper<TouchUpEvent>() {
      var Entity.TouchUpEventOrNull by optionalPropertyFor(mapper)
    }
  }

  data class TouchDraggedEvent(val screenX: Int, val screenY: Int, val pointer: Int) : InputEvent {
    companion object : Mapper<TouchDraggedEvent>() {
      var Entity.TouchDraggedEventOrNull by optionalPropertyFor(mapper)
    }
  }

  data class MouseMovedEvent(val screenX: Int, val screenY: Int) : InputEvent {
    companion object : Mapper<MouseMovedEvent>() {
      var Entity.mouseMovedEventOrNull by optionalPropertyFor(mapper)
    }
  }

  data class ScrolledEvent(val amountX: Float, val amountY: Float) : InputEvent {
    companion object : Mapper<ScrolledEvent>() {
      var Entity.scrolledEventOrNull by optionalPropertyFor(mapper)
    }
  }
}
