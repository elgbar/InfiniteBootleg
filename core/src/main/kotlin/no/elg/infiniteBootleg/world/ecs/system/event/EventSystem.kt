package no.elg.infiniteBootleg.world.ecs.system.event

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_EVENT_HANDLING
import no.elg.infiniteBootleg.world.ecs.components.events.ECSEvent
import no.elg.infiniteBootleg.world.ecs.components.events.ECSEventQueueComponent
import kotlin.reflect.KClass

abstract class EventSystem<T : ECSEvent, Q : ECSEventQueueComponent<T>>(
  family: Family,
  eventType: KClass<T>,
  private val queueMapper: ComponentMapper<out Q>
) : IteratingSystem(family, UPDATE_PRIORITY_EVENT_HANDLING) {

  init {
    require(eventType.isSealed) { "Event components must be sealed types. The type $eventType is not a sealed type!" }
  }

  final override fun processEntity(entity: Entity, deltaTime: Float) {
    queueMapper.get(entity)?.also {
      val events = it.events
      while (events.isNotEmpty()) {
        handleEvent(entity, deltaTime, events.poll())
      }
    }
  }

  abstract fun handleEvent(entity: Entity, deltaTime: Float, event: T)
}
