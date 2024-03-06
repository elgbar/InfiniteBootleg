package no.elg.infiniteBootleg.input

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.world.ecs.components.InputEventQueueComponent.Companion.queueInputEvent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent
import no.elg.infiniteBootleg.world.world.World

class ECSInputListener(val world: World) : InputProcessor, Disposable {

  fun handleEvent(inputEvent: InputEvent): Boolean {
    world.engine.queueInputEvent(inputEvent) {
      ClientMain.inst().shouldNotIgnoreWorldInput()
    }
    return false
  }

  private val keysDownSet = mutableSetOf<Int>()
  val keysDown: Set<Int> = keysDownSet

  override fun keyDown(keycode: Int): Boolean {
    keysDownSet += keycode
    return handleEvent(InputEvent.KeyDownEvent(keycode))
  }

  override fun keyUp(keycode: Int): Boolean {
    keysDownSet -= keycode
    // handleEvent(InputEvent.KeyUpEvent(keycode))
    return false
  }

  override fun keyTyped(character: Char): Boolean = false // handleEvent(InputEvent.KeyTypedEvent(character))
  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = handleEvent(InputEvent.TouchDownEvent(screenX, screenY, pointer, button))
  override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false // handleEvent(InputEvent.TouchUpEvent(screenX, screenY, pointer, button))
  override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean =
    false // handleEvent(InputEvent.TouchCancelledEvent(screenX, screenY, pointer, button))

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = false // handleEvent(InputEvent.TouchDraggedEvent(screenX, screenY, pointer))
  override fun mouseMoved(screenX: Int, screenY: Int): Boolean = false // handleEvent(InputEvent.MouseMovedEvent(screenX, screenY))
  override fun scrolled(amountX: Float, amountY: Float): Boolean = handleEvent(InputEvent.ScrolledEvent(amountX, amountY))

  override fun dispose() {
    ClientMain.inst().inputMultiplexer.removeProcessor(this)
  }
}
