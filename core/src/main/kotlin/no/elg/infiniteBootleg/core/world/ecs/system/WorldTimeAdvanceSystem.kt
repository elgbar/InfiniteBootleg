package no.elg.infiniteBootleg.core.world.ecs.system

import com.badlogic.ashley.core.EntitySystem
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_EARLY
import no.elg.infiniteBootleg.core.world.world.World

class WorldTimeAdvanceSystem(private val world: World) : EntitySystem(UPDATE_PRIORITY_EARLY) {
  override fun update(deltaTime: Float) {
    val time = world.worldTime
    time.time += time.timeScale * deltaTime
  }
}
