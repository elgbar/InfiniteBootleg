package no.elg.infiniteBootleg.core.world.ecs.system.api

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_EVENT_HANDLING
import no.elg.infiniteBootleg.core.world.ecs.components.events.ECSEvent
import no.elg.infiniteBootleg.core.world.ecs.components.events.ECSEventQueueComponent
import kotlin.reflect.KClass

abstract class EventSystem<T : ECSEvent, Q : ECSEventQueueComponent<T>>(family: Family, eventType: KClass<T>, private val queueMapper: ComponentMapper<out Q>) :
  ConditionalIteratingSystem(family, UPDATE_PRIORITY_EVENT_HANDLING) {

  init {
    require(eventType.isSealed) { "Event components must be sealed types. The type $eventType is not a sealed type!" }
  }

  override fun condition(entity: Entity): Boolean = true

  final override fun processEntity(entity: Entity, deltaTime: Float) {
    val eventQueueComponent = queueMapper.get(entity) ?: return
    eventQueueComponent.processEvents(entity, ::handleEvent)
  }

  abstract fun handleEvent(entity: Entity, event: T)
}
