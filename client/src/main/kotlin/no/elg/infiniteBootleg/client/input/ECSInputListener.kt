package no.elg.infiniteBootleg.client.input

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.core.world.ecs.components.InputEventQueueComponent.Companion.queueInputEventAsync
import no.elg.infiniteBootleg.core.world.ecs.components.events.InputEvent
import no.elg.infiniteBootleg.core.world.world.World
import java.util.concurrent.CopyOnWriteArraySet

class ECSInputListener(val world: World) :
  InputProcessor,
  Disposable {

  fun handleEvent(inputEvent: InputEvent): Boolean {
    world.engine.queueInputEventAsync(inputEvent) {
      ClientMain.inst().shouldNotIgnoreWorldInput()
    }
    return false
  }

  private val buttonsDownSet: MutableSet<Int> = CopyOnWriteArraySet()
  private val keysDownSet: MutableSet<Int> = CopyOnWriteArraySet()

  val keysDown: Set<Int> get() = keysDownSet

  override fun keyDown(keycode: Int): Boolean {
    keysDownSet += keycode
    handleEvent(InputEvent.KeyDownEvent(keycode))
    return handleEvent(InputEvent.KeyIsDownEvent(keycode))
  }

  override fun keyUp(keycode: Int): Boolean {
    keysDownSet -= keycode
    // handleEvent(InputEvent.KeyUpEvent(keycode))
    return false
  }

  override fun keyTyped(character: Char): Boolean = false // handleEvent(InputEvent.KeyTypedEvent(character))
  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    buttonsDownSet += button
    handleEvent(InputEvent.TouchDownEvent(screenX, screenY, pointer, button))
    return handleEvent(InputEvent.TouchDraggedEvent(screenX, screenY, pointer, buttonsDownSet, justPressed = true))
  }

  override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    buttonsDownSet -= button
    // handleEvent(InputEvent.TouchUpEvent(screenX, screenY, pointer, button))
    return false
  }

  override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean =
    false // handleEvent(InputEvent.TouchCancelledEvent(screenX, screenY, pointer, button))

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean =
    handleEvent(InputEvent.TouchDraggedEvent(screenX, screenY, pointer, buttonsDownSet, justPressed = false))

  override fun mouseMoved(screenX: Int, screenY: Int): Boolean = false // handleEvent(InputEvent.MouseMovedEvent(screenX, screenY))
  override fun scrolled(amountX: Float, amountY: Float): Boolean = handleEvent(InputEvent.ScrolledEvent(amountX, amountY))

  override fun dispose() {
    ClientMain.inst().inputMultiplexer.removeProcessor(this)
  }
}
