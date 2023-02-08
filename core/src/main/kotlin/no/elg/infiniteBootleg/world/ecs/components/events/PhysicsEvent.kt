package no.elg.infiniteBootleg.world.ecs.components.events

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.Manifold
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import java.util.concurrent.ConcurrentLinkedQueue

class PhysicsEventQueue : ECSEventQueue<PhysicsEvent> {
  override val events = ConcurrentLinkedQueue<PhysicsEvent>()

  companion object : Mapper<PhysicsEventQueue>() {
    var Entity.physicsEventQueueOrNull by optionalPropertyFor(mapper)
  }
}

sealed interface PhysicsEvent : ECSEvent {

  val fixtureA: Fixture
  val fixtureB: Fixture

  data class ContactBeginsEvent(override val fixtureA: Fixture, override val fixtureB: Fixture) : PhysicsEvent

  data class ContactEndsEvent(override val fixtureA: Fixture, override val fixtureB: Fixture) : PhysicsEvent

  data class PreSolveContactEvent(override val fixtureA: Fixture, override val fixtureB: Fixture, val oldManifold: Manifold) : PhysicsEvent

  data class PostSolveContactEvent(override val fixtureA: Fixture, override val fixtureB: Fixture, val impulse: ContactImpulse) : PhysicsEvent
}
