package no.elg.infiniteBootleg.world.ecs.system.event

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.world.ecs.components.events.ECSEvent
import no.elg.infiniteBootleg.world.ecs.components.events.ECSEventQueue
import kotlin.reflect.KClass

abstract class EventSystem<T : ECSEvent, Q : ECSEventQueue<T>>(
  family: Family,
  priority: Int,
  eventType: KClass<T>,
  private val queueMapper: ComponentMapper<out Q>
) :
  IteratingSystem(family, priority) {

  private val sealedSubclasses = eventType.sealedSubclasses

  init {
    if (!eventType.isSealed) {
      error("Event components must be sealed types. The type ${eventType.simpleName} is not a sealed type!")
    }
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
