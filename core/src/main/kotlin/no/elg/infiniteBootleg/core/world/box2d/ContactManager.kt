package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Manifold
import no.elg.infiniteBootleg.core.world.ecs.components.PhysicsEventQueueComponent.Companion.queuePhysicsEvent
import no.elg.infiniteBootleg.core.world.ecs.components.events.PhysicsEvent

class ContactManager(val engine: Engine) : ContactListener {

  private fun queuePhysicsEvent(event: PhysicsEvent) {
    engine.queuePhysicsEvent(event) { entity -> event.fixtureA?.body?.userData === entity || event.fixtureB?.body?.userData === entity }
  }

  override fun beginContact(contact: Contact) = queuePhysicsEvent(PhysicsEvent.ContactBeginsEvent(contact.fixtureA, contact.fixtureB))
  override fun endContact(contact: Contact) = queuePhysicsEvent(PhysicsEvent.ContactEndsEvent(contact.fixtureA, contact.fixtureB))
  override fun preSolve(contact: Contact, oldManifold: Manifold) =
    Unit // queuePhysicsEvent(PhysicsEvent.PreSolveContactEvent(contact.fixtureA, contact.fixtureB, oldManifold), contact)

  override fun postSolve(contact: Contact, impulse: ContactImpulse) =
    Unit // queuePhysicsEvent(PhysicsEvent.PostSolveContactEvent(contact.fixtureA, contact.fixtureB, impulse), contact)
}
