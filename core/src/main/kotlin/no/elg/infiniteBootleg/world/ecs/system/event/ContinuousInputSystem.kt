package no.elg.infiniteBootleg.world.ecs.system.event

import com.badlogic.ashley.core.EntitySystem
import no.elg.infiniteBootleg.input.ECSInputListener
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_EVENT_HANDLING
import no.elg.infiniteBootleg.world.ecs.api.restriction.system.ClientSystem
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent

class ContinuousInputSystem(private val ecsInput: ECSInputListener) : EntitySystem(UPDATE_PRIORITY_EVENT_HANDLING + 1), ClientSystem {

  override fun update(deltaTime: Float) {
    for (keycode in ecsInput.keysDown) {
      ecsInput.handleEvent(InputEvent.KeyIsDownEvent(keycode))
    }
  }
}
