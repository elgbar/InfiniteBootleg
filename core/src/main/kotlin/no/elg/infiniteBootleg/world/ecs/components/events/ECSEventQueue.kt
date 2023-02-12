package no.elg.infiniteBootleg.world.ecs.components.events

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import ktx.ashley.allOf
import no.elg.infiniteBootleg.world.ecs.BASIC_DYNAMIC_ENTITY_ARRAY
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * The queue of events to be processed by an entity, we cannot map events directly onto entities as multiple events might occur between processing
 */
interface ECSEventQueue<T : ECSEvent> : Component {

  val events: ConcurrentLinkedQueue<T>

  companion object {
    inline fun <T : ECSEvent, reified Q : ECSEventQueue<T>> Engine.queueEvent(queueMapper: ComponentMapper<out Q>, event: T, filter: (Entity) -> Boolean = { true }) {
      val family = allOf(*BASIC_DYNAMIC_ENTITY_ARRAY, Q::class).get()
      this.getEntitiesFor(family).filter(filter).forEach {
        val ecsEvents = queueMapper.get(it) ?: return@forEach
        ecsEvents.events += event
      }
    }
  }
}
