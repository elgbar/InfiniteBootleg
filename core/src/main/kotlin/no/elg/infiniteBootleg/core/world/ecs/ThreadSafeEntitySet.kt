package no.elg.infiniteBootleg.core.world.ecs

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import no.elg.infiniteBootleg.core.util.isInvalid
import java.util.concurrent.CopyOnWriteArraySet

/**
 * An [EntityListener] that maintains a thread-safe [Set] of entities.
 * Register on an [com.badlogic.ashley.core.Engine] with a [com.badlogic.ashley.core.Family] to track entities matching that family.
 * The [entities] set can be safely read from any thread.
 */
class ThreadSafeEntitySet : EntityListener {

  private val _entities = CopyOnWriteArraySet<Entity>()
  val entities: Set<Entity> get() = _entities

  override fun entityAdded(entity: Entity?) {
    if (entity != null) {
      _entities += entity
    }
    _entities.removeIf { it.isInvalid }
  }

  override fun entityRemoved(entity: Entity?) {
    if (entity != null) {
      _entities -= entity
    }
    _entities.removeIf { it.isInvalid }
  }
}
