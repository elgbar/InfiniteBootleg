package no.elg.infiniteBootleg.core.world.ecs.system

import com.badlogic.ashley.systems.IntervalSystem
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_LATE
import no.elg.infiniteBootleg.core.world.world.World

class WorldTimeAdvanceSystem(private val world: World) : IntervalSystem(WORLD_TIME_INTERVAL_SECONDS, UPDATE_PRIORITY_LATE) {

  override fun updateInterval() {
    val time = world.worldTime
    time.time += time.timeScale * 1 / 30f
  }

  companion object {
    private const val WORLD_TIME_INTERVAL_SECONDS = 1f
  }
}
