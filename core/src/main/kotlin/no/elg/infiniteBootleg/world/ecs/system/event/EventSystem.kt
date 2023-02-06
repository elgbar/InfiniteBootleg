package no.elg.infiniteBootleg.world.ecs.system.event

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.world.ecs.components.events.ECSEvent
import kotlin.reflect.KClass

abstract class EventSystem<T : ECSEvent>(family: Family, priority: Int, eventType: KClass<T>) : IteratingSystem(family, priority) {

  private val sealedSubclasses = eventType.sealedSubclasses

  init {
    if (!eventType.isSealed) {
      error("Event components must be sealed types. The type ${eventType.simpleName} is not a sealed type!")
    }
  }

  final override fun processEntity(entity: Entity, deltaTime: Float) {
    handleEvent(entity, deltaTime)
    for (subclass in sealedSubclasses) {
      entity.remove(subclass.java)
    }
  }

  abstract fun handleEvent(entity: Entity, deltaTime: Float)

  inline fun <E : T> handleInputEvent(entity: Entity, mapper: ComponentMapper<out E>, action: (E) -> Unit) {
    mapper.get(entity)?.also(action)
  }
}
