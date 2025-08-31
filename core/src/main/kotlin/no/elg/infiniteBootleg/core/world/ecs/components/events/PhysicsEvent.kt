package no.elg.infiniteBootleg.core.world.ecs.components.events

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.box2d.structs.b2ShapeId
import no.elg.infiniteBootleg.core.world.box2d.extensions.body
import no.elg.infiniteBootleg.core.world.box2d.extensions.isValid
import no.elg.infiniteBootleg.core.world.box2d.extensions.userData

/**
 * [shapeIdA] and [shapeIdB] are shared instances, so copy the data you need from them
 */
sealed class PhysicsEvent(shapeIdA: b2ShapeId?, shapeIdB: b2ShapeId?) : ECSEvent {

  val entityA: Entity? = findValidEntity(shapeIdA)
  val entityB: Entity? = findValidEntity(shapeIdB)

  fun isValid(): Boolean = entityA != null || entityB != null

  fun getOtherEventEntity(eventEntity: Entity): Entity? = if (eventEntity === entityA) entityB else entityA

  class ContactBeginsEvent(shapeIdA: b2ShapeId?, shapeIdB: b2ShapeId?) : PhysicsEvent(shapeIdA, shapeIdB)

  class ContactEndsEvent(shapeIdA: b2ShapeId?, shapeIdB: b2ShapeId?) : PhysicsEvent(shapeIdA, shapeIdB)

  /**
   * Material of block changed
   */
  class BlockRemovedEvent(shapeIdA: b2ShapeId?, val compactLocation: Long) : PhysicsEvent(shapeIdA, null)

  companion object {
    fun findValidEntity(shapeId: b2ShapeId?): Entity? {
      if (shapeId == null || !shapeId.isValid) return null
      val body = shapeId.body
      if (!body.isValid) return null
      return body.userData as? Entity
    }
  }
}
