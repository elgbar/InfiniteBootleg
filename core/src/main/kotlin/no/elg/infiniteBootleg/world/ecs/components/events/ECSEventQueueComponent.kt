package no.elg.infiniteBootleg.world.ecs.components.events

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import ktx.ashley.allOf
import ktx.collections.getOrPut
import no.elg.infiniteBootleg.world.ecs.BASIC_STANDALONE_ENTITY
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.KClass

/**
 * The queue of events to be processed by an entity, we cannot map events directly onto entities as multiple events might occur between processing
 */
interface ECSEventQueueComponent<T : ECSEvent> : EntitySavableComponent, Pool.Poolable {

  val events: ConcurrentLinkedQueue<T>

  override fun reset() {
    events.clear()
  }

  companion object {

    val entitiesCache = ObjectMap<KClass<out ECSEventQueueComponent<out ECSEvent>>, ImmutableArray<Entity>>()

    inline fun <T : ECSEvent, reified Q : ECSEventQueueComponent<T>> Engine.queueEvent(
      queueMapper: ComponentMapper<out Q>,
      event: T,
      noinline filter: (Entity) -> Boolean = { true }
    ) {
      val entities = entitiesCache.getOrPut(Q::class) { getEntitiesFor(allOf(*BASIC_STANDALONE_ENTITY, Q::class).get()) }
      entities.asSequence().filter(filter).forEach {
        val ecsEvents = queueMapper.get(it) ?: return@forEach
        ecsEvents.events += event
      }
    }
  }
}
