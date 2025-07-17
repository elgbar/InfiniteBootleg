// package no.elg.infiniteBootleg.core.world.ecs.components.events
//
// import com.badlogic.gdx.box2d.structs.b2ShapeId
// import com.badlogic.gdx.physics.box2d.ContactImpulse
// import com.badlogic.gdx.physics.box2d.Fixture
// import com.badlogic.gdx.physics.box2d.Manifold
//
// sealed interface PhysicsEvent : ECSEvent {
//
//  val fixtureA: b2ShapeId?
//  val fixtureB: Fixture?
//
//  data class ContactBeginsEvent(override val fixtureA: Fixture, override val fixtureB: Fixture) : PhysicsEvent
//
//  data class ContactEndsEvent(override val fixtureA: Fixture, override val fixtureB: Fixture) : PhysicsEvent
//
//  data class PreSolveContactEvent(override val fixtureA: Fixture, override val fixtureB: Fixture, val oldManifold: Manifold) : PhysicsEvent
//
//  data class PostSolveContactEvent(override val fixtureA: Fixture, override val fixtureB: Fixture, val impulse: ContactImpulse) : PhysicsEvent
//
//  /**
//   * Material of block changed
//   */
//  data class BlockRemovedEvent(override val fixtureA: Fixture, val compactLocation: Long) : PhysicsEvent {
//    override val fixtureB: Fixture? = null
//  }
// }
