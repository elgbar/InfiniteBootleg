package no.elg.infiniteBootleg.core.world.ecs.system

import com.badlogic.ashley.systems.IntervalSystem
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.events.WorldTickedEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_EVENT_HANDLING
import no.elg.infiniteBootleg.core.world.world.World

/**
 * Temp solution to dive [WorldTickedEvent]
 */
class WorldTickSystem(val world: World) : IntervalSystem(1f / Settings.tps, UPDATE_PRIORITY_EVENT_HANDLING - 1) {

  private val worldTickedEvent by lazy { WorldTickedEvent(world) }

  override fun updateInterval() {
    EventManager.dispatchEvent(worldTickedEvent)
  }
}
