package no.elg.infiniteBootleg.core.world.ecs.system.api

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.utils.Array

/**
 * Process a whole family of entities and not just one at a time (which [com.badlogic.ashley.systems.IteratingSystem] does)
 *
 * @see com.badlogic.ashley.systems.IteratingSystem
 */
abstract class FamilyEntitySystem(private val family: Family, priority: Int) : EntitySystem(priority) {

  private var entities: ImmutableArray<Entity> = EMPTY_ENTITIES

  override fun addedToEngine(engine: Engine) {
    entities = engine.getEntitiesFor(family)
  }

  override fun removedFromEngine(engine: Engine?) {
    entities = EMPTY_ENTITIES
  }

  final override fun update(deltaTime: Float) {
    if (entities.size() == 0) return
    processEntities(entities, deltaTime)
  }

  abstract fun processEntities(entities: ImmutableArray<Entity>, deltaTime: Float)

  companion object {
    private val EMPTY_ENTITIES = ImmutableArray<Entity>(Array.with())
  }
}
