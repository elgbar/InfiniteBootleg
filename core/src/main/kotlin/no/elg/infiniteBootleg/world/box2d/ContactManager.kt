package no.elg.infiniteBootleg.world.box2d

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Manifold
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue.Companion.queuePhysicsEvent

class ContactManager(val engine: Engine) : ContactListener {

  private fun Contact.containsEntityFixture(entity: Entity): Boolean {
    return fixtureA.body.userData === entity || fixtureB.body.userData === entity
  }

  private fun queuePhysicsEvent(event: PhysicsEvent, contact: Contact) {
    engine.queuePhysicsEvent(event) { contact.containsEntityFixture(it) }
//    engine.queueEvent(PhysicsEventQueue::class, PhysicsEventQueue.mapper, event) { contact.containsEntityFixture(it) }
  }

  override fun beginContact(contact: Contact) = queuePhysicsEvent(PhysicsEvent.ContactBeginsEvent(contact.fixtureA, contact.fixtureB), contact)
  override fun endContact(contact: Contact) = queuePhysicsEvent(PhysicsEvent.ContactEndsEvent(contact.fixtureA, contact.fixtureB), contact)
  override fun preSolve(contact: Contact, oldManifold: Manifold) =
    Unit // queuePhysicsEvent(PhysicsEvent.PreSolveContactEvent(contact.fixtureA, contact.fixtureB, oldManifold), contact)

  override fun postSolve(contact: Contact, impulse: ContactImpulse) =
    Unit // queuePhysicsEvent(PhysicsEvent.PostSolveContactEvent(contact.fixtureA, contact.fixtureB, impulse), contact)
}
