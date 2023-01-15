package no.elg.infiniteBootleg.input

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.utils.Disposable
import ktx.ashley.configureEntity
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.ecs.components.ControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent
import no.elg.infiniteBootleg.world.ecs.toFamily
import no.elg.infiniteBootleg.world.ecs.with

class ECSInputListener(val world: World) : InputProcessor, Disposable {

  private fun handleEvent(inputEvent: InputEvent): Boolean {
    if (!Main.inst().console.isVisible) {
      world.engine.getEntitiesFor(ControlledComponent.LocallyControlledComponent::class.toFamily()).forEach {
        world.engine.configureEntity(it) {
          with(inputEvent)
        }
      }
    }
    return false
  }

  override fun keyDown(keycode: Int): Boolean = handleEvent(InputEvent.KeyDownEvent(keycode))

  override fun keyUp(keycode: Int): Boolean = handleEvent(InputEvent.KeyUpEvent(keycode))

  override fun keyTyped(character: Char): Boolean = handleEvent(InputEvent.KeyTypedEvent(character))

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = handleEvent(InputEvent.TouchDownEvent(screenX, screenY, pointer, button))

  override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = handleEvent(InputEvent.TouchUpEvent(screenX, screenY, pointer, button))

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = handleEvent(InputEvent.TouchDraggedEvent(screenX, screenY, pointer))

  override fun mouseMoved(screenX: Int, screenY: Int): Boolean = handleEvent(InputEvent.MouseMovedEvent(screenX, screenY))

  override fun scrolled(amountX: Float, amountY: Float): Boolean = handleEvent(InputEvent.ScrolledEvent(amountX, amountY))

  override fun dispose() {
    ClientMain.inst().inputMultiplexer.removeProcessor(this)
  }
}
