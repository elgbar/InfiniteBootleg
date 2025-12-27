package no.elg.infiniteBootleg.core.world.ecs.components.events

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import io.github.oshai.kotlinlogging.KotlinLogging
import it.unimi.dsi.fastutil.Hash.DEFAULT_LOAD_FACTOR
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import ktx.ashley.allOf
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.events.api.ThreadType.Companion.requireCorrectThreadType
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.launchOnBox2d
import no.elg.infiniteBootleg.core.world.ecs.BASIC_STANDALONE_ENTITY
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.core.world.world.World
import kotlin.reflect.KClass

/**
 * The queue of events to be processed by an entity, we cannot map events directly onto entities as multiple events might occur between processing
 */
abstract class ECSEventQueueComponent<T : ECSEvent> : EntitySavableComponent {
  /**
   * Must only be accessed on the box2d thread!
   */
  private val events = ObjectOpenHashSet<T>(0, DEFAULT_LOAD_FACTOR)

  override fun hudDebug(): String = "Event queue size: ${events.size}"

  /**
   * Enqueue an event from the physics thread
   */
  fun enqueue(event: T) {
    requireCorrectThreadType(ThreadType.PHYSICS) { "Event queue must be accessed on the ${ThreadType.PHYSICS} thread" }
    events.add(event)
  }

  fun processEvents(entity: Entity, processEvent: (entity: Entity, event: T) -> Unit) {
    requireCorrectThreadType(ThreadType.PHYSICS) { "Event queue must be accessed on the ${ThreadType.PHYSICS} thread" }
    for (event: T in events) {
      processEvent(entity, event)
    }
    events.clear()
  }

  companion object {

    /**
     * Optimization to skip filtering
     */
    val ALLOW_ALL_FILTER: (Entity) -> Boolean = { _: Entity -> true }

    val entitiesCache = Object2ObjectOpenHashMap<KClass<out ECSEventQueueComponent<out ECSEvent>>, ImmutableArray<Entity>>()

    inline fun <T : ECSEvent, reified Q : ECSEventQueueComponent<T>> World.queueEventAsync(
      queueMapper: ComponentMapper<out Q>,
      event: T,
      noinline filter: (Entity) -> Boolean = ALLOW_ALL_FILTER
    ) {
      launchOnBox2d { engine.queueEvent(queueMapper, event, filter) }
    }

    inline fun <T : ECSEvent, reified Q : ECSEventQueueComponent<T>> Engine.queueEvent(
      queueMapper: ComponentMapper<out Q>,
      event: T,
      noinline filter: (Entity) -> Boolean = ALLOW_ALL_FILTER
    ) {
      if (Main.inst().world?.worldTicker?.isPaused != false) {
        KotlinLogging.logger {}.debug { "Dropping queued event as the world ticker is paused" }
        return
      }
      val entities = entitiesCache.getOrPut(Q::class) { getEntitiesFor(allOf(*BASIC_STANDALONE_ENTITY, Q::class).get()) }
      for (entity in entities) {
        if (filter === ALLOW_ALL_FILTER || filter(entity)) {
          val ecsEvents = queueMapper.get(entity) ?: continue
          ecsEvents.enqueue(event)
        }
      }
    }
  }
}
