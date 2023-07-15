package no.elg.infiniteBootleg.world.ecs.components.events

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.Manifold
import com.badlogic.gdx.utils.Pool
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.world.ecs.components.events.ECSEventQueue.Companion.queueEvent
import java.util.concurrent.ConcurrentLinkedQueue

class PhysicsEventQueue : ECSEventQueue<PhysicsEvent>, Pool.Poolable {
  override val events = ConcurrentLinkedQueue<PhysicsEvent>()

  companion object : Mapper<PhysicsEventQueue>() {
    var Entity.physicsEventQueueOrNull by optionalPropertyFor(mapper)
    inline fun Engine.queuePhysicsEvent(event: PhysicsEvent, filter: (Entity) -> Boolean = { true }) {
      queueEvent(PhysicsEventQueue.mapper, event, filter)
    }
  }

  override fun reset() {
    events.clear()
  }
}

sealed interface PhysicsEvent : ECSEvent {

  val fixtureA: Fixture?
  val fixtureB: Fixture?

  data class ContactBeginsEvent(override val fixtureA: Fixture, override val fixtureB: Fixture) : PhysicsEvent

  data class ContactEndsEvent(override val fixtureA: Fixture, override val fixtureB: Fixture) : PhysicsEvent

  data class PreSolveContactEvent(override val fixtureA: Fixture, override val fixtureB: Fixture, val oldManifold: Manifold) : PhysicsEvent

  data class PostSolveContactEvent(override val fixtureA: Fixture, override val fixtureB: Fixture, val impulse: ContactImpulse) : PhysicsEvent

  /**
   * Material of block changed
   */
  class BlockRemovedEvent(override val fixtureA: Fixture, val compactLocation: Long) : PhysicsEvent {
    override val fixtureB: Fixture? = null
  }
}
