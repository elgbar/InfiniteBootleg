package no.elg.infiniteBootleg.world.ecs.components.events

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import ktx.ashley.allOf
import no.elg.infiniteBootleg.world.ecs.CONTROLLED_ENTITY_ARRAY
import kotlin.reflect.KClass

interface ECSEvent {

  companion object {
    inline fun <T : ECSEvent, Q : ECSEventQueue<T>> Engine.queueEvent(
      eventList: KClass<out Q>,
      queueMapper: ComponentMapper<out Q>,
      event: T,
      filter: (Entity) -> Boolean = { true }
    ) {
      val family = allOf(*CONTROLLED_ENTITY_ARRAY, eventList).get()
      this.getEntitiesFor(family).filter(filter).forEach {
        val ecsEvents = queueMapper.get(it) ?: return@forEach
        ecsEvents.events += event
      }
    }
  }
}
