package no.elg.infiniteBootleg.core.world.ecs.system.api

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.utils.Array

/**
 * Conditionally process entities in a family
 *
 * @see com.badlogic.ashley.systems.IteratingSystem
 */
abstract class ConditionalIteratingSystem(private val family: Family, priority: Int) : EntitySystem(priority) {

  private var entities: ImmutableArray<Entity> = EMPTY_ENTITIES

  override fun addedToEngine(engine: Engine) {
    entities = engine.getEntitiesFor(family)
  }

  override fun removedFromEngine(engine: Engine?) {
    entities = EMPTY_ENTITIES
  }

  final override fun update(deltaTime: Float) {
    if (entities.size() == 0) return
    val filteredEntities: Sequence<Entity> = entities.asSequence().filter(::condition)
    if (filteredEntities.none()) return
    beforeAllHandle()
    for (entity in filteredEntities) {
      processEntity(entity, deltaTime)
    }
    afterAllHandle()
  }

  /**
   * Check if the entity should be processed
   *
   * @param entity The entity to check
   * @return true if the entity should be processed, false otherwise
   */
  abstract fun condition(entity: Entity): Boolean

  abstract fun processEntity(entity: Entity, deltaTime: Float)

  /**
   * Called before processing all entities
   *
   * Will not be called if no entities are processed
   *
   * @see afterAllHandle
   */
  open fun beforeAllHandle(): Unit = Unit

  /**
   * Called after processing all entities
   *
   * Will not be called if no entities are processed
   *
   * @see beforeAllHandle
   */
  open fun afterAllHandle(): Unit = Unit

  companion object {
    private val EMPTY_ENTITIES = ImmutableArray<Entity>(Array.with())
  }
}
