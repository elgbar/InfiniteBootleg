package no.elg.infiniteBootleg.core.world.ecs.components.events

import no.elg.infiniteBootleg.core.world.Staff

sealed interface InputEvent : ECSEvent {

  interface MouseInputEvent {
    val screenX: Int
    val screenY: Int
  }

  /**
   * Key was just pressed down
   */
  data class KeyDownEvent(val keycode: Int) : InputEvent

  /**
   * Key is being held down. Will be fired on the first frame it was pushed down, together with [KeyDownEvent]
   */
  data class KeyIsDownEvent(val keycode: Int) : InputEvent

  data class KeyUpEvent(val keycode: Int) : InputEvent

  data class KeyTypedEvent(val char: Char) : InputEvent

  data class TouchDownEvent(override val screenX: Int, override val screenY: Int, val pointer: Int, val button: Int) :
    InputEvent,
    MouseInputEvent

  data class TouchUpEvent(override val screenX: Int, override val screenY: Int, val pointer: Int, val button: Int) :
    InputEvent,
    MouseInputEvent

  data class TouchDraggedEvent(override val screenX: Int, override val screenY: Int, val pointer: Int, val buttons: Set<Int>, val justPressed: Boolean) :
    InputEvent,
    MouseInputEvent

  data class MouseMovedEvent(override val screenX: Int, override val screenY: Int) :
    InputEvent,
    MouseInputEvent

  data class ScrolledEvent(val amountX: Float, val amountY: Float) : InputEvent

  data class SpellCastEvent(val staff: Staff) : InputEvent
}
