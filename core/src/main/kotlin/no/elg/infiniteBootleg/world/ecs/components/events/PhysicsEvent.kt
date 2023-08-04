package no.elg.infiniteBootleg.world.ecs.components.events

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.Manifold
import com.badlogic.gdx.utils.Pool
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.with
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ecs.api.StatelessAdditionalComponentsLoadableMapper
import no.elg.infiniteBootleg.world.ecs.components.events.ECSEventQueue.Companion.queueEvent
import java.util.concurrent.ConcurrentLinkedQueue

class PhysicsEventQueue : ECSEventQueue<PhysicsEvent>, Pool.Poolable {
  override val events = ConcurrentLinkedQueue<PhysicsEvent>()

  override fun EntityKt.AdditionalComponentsKt.Dsl.save() {
    physicsEvent = true
  }

  companion object : StatelessAdditionalComponentsLoadableMapper<PhysicsEventQueue>() {
    var Entity.physicsEventQueueOrNull by optionalPropertyFor(mapper)
    fun Engine.queuePhysicsEvent(event: PhysicsEvent, filter: (Entity) -> Boolean = { true }) {
      queueEvent(PhysicsEventQueue.mapper, event, filter)
    }

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.AdditionalComponents): PhysicsEventQueue = with<PhysicsEventQueue>()
    override fun ProtoWorld.Entity.AdditionalComponents.checkShouldLoad(): Boolean = hasPhysicsEvent()
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
