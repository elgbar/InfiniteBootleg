package no.elg.infiniteBootleg.world.subgrid.contact

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Manifold
import no.elg.infiniteBootleg.world.ecs.components.events.ECSEvent.Companion.handleEvent
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEvent

class ContactManager(val engine: Engine) : ContactListener {

  override fun beginContact(contact: Contact) = engine.handleEvent(PhysicsEvent.ContactBeginsEvent(contact)) // { contact.containsEntityFixture(it) }
  override fun endContact(contact: Contact) = engine.handleEvent(PhysicsEvent.ContactEndsEvent(contact)) // { contact.containsEntityFixture(it) }
  override fun preSolve(contact: Contact, oldManifold: Manifold) =
    engine.handleEvent(PhysicsEvent.PreSolveContactEvent(contact, oldManifold)) // { contact.containsEntityFixture(it) }

  override fun postSolve(contact: Contact, impulse: ContactImpulse) =
    engine.handleEvent(PhysicsEvent.PostSolveContactEvent(contact, impulse)) // { contact.containsEntityFixture(it) }
}
