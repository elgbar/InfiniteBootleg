package no.elg.infiniteBootleg.client.world.ecs.system.event

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.EntitySystem
import no.elg.infiniteBootleg.client.input.ECSInputListener
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.core.world.ecs.AFTER
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_EVENT_HANDLING
import no.elg.infiniteBootleg.core.world.ecs.components.events.InputEvent

class ContinuousInputSystem(private val ecsInput: ECSInputListener) : EntitySystem(AFTER + UPDATE_PRIORITY_EVENT_HANDLING) {

  override fun update(deltaTime: Float) {
    for (keycode in ecsInput.keysDown) {
      ecsInput.handleEvent(InputEvent.KeyIsDownEvent(keycode))
    }
  }

  override fun addedToEngine(engine: Engine) {
    ClientMain.inst().inputMultiplexer.addProcessor(ecsInput)
  }

  override fun removedFromEngine(engine: Engine?) {
    ecsInput.dispose()
  }
}
