package no.elg.infiniteBootleg.world.ecs.components.events

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.Manifold
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor

sealed interface PhysicsEvent : ECSEvent {

  data class ContactBeginsEvent(val contact: Contact) : PhysicsEvent {
    companion object : Mapper<ContactBeginsEvent>() {
      var Entity.contactBeginsEvent by optionalPropertyFor(ContactBeginsEvent.mapper)
    }
  }

  data class ContactEndsEvent(val contact: Contact) : PhysicsEvent {
    companion object : Mapper<ContactEndsEvent>() {
      var Entity.contactEndsEvent by optionalPropertyFor(ContactEndsEvent.mapper)
    }
  }

  data class PreSolveContactEvent(val contact: Contact, val oldManifold: Manifold) : PhysicsEvent {
    companion object : Mapper<PreSolveContactEvent>() {
      var Entity.preSolveContactEvent by optionalPropertyFor(PreSolveContactEvent.mapper)
    }
  }

  data class PostSolveContactEvent(val contact: Contact, val impulse: ContactImpulse) : PhysicsEvent {
    companion object : Mapper<PostSolveContactEvent>() {
      var Entity.postSolveContactEvent by optionalPropertyFor(PostSolveContactEvent.mapper)
    }
  }
}
