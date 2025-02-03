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
    entities.filter(::condition).forEach { processEntity(it, deltaTime) }
  }

  abstract fun condition(entity: Entity): Boolean

  abstract fun processEntity(entity: Entity, deltaTime: Float)

  companion object {
    private val EMPTY_ENTITIES = ImmutableArray<Entity>(Array.with())
  }
}
