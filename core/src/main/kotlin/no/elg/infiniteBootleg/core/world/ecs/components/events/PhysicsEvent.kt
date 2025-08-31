package no.elg.infiniteBootleg.core.world.ecs.components.events

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.box2d.structs.b2ShapeId
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.box2d.extensions.body
import no.elg.infiniteBootleg.core.world.box2d.extensions.isValid
import no.elg.infiniteBootleg.core.world.box2d.extensions.userData

/**
 * [shapeIdA] and [shapeIdB] are shared instances, so copy the data you need from them
 */
sealed class PhysicsEvent(shapeIdA: b2ShapeId?, shapeIdB: b2ShapeId?) : ECSEvent {

  val entityA: Entity? = findBodyUserData(shapeIdA) as? Entity
  val entityB: Entity? = findBodyUserData(shapeIdB) as? Entity
  val blockA: Block? = findBodyUserData(shapeIdA) as? Block
  val blockB: Block? = findBodyUserData(shapeIdB) as? Block
  val userdataA: Any? = findShapeUserData(shapeIdA)
  val userdataB: Any? = findShapeUserData(shapeIdB)

  fun isValid(): Boolean = entityA != null || entityB != null

  fun getOtherEventEntity(eventEntity: Entity): Entity? = if (eventEntity === entityA) entityB else entityA
  fun getOtherEventBlock(eventEntity: Entity): Block? = if (eventEntity === entityA) blockB else blockA
  fun getOtherUserData(eventEntity: Entity): Any? = if (eventEntity === entityA) userdataB else userdataA

  class ContactBeginsEvent(shapeIdA: b2ShapeId?, shapeIdB: b2ShapeId?) : PhysicsEvent(shapeIdA, shapeIdB)

  class ContactEndsEvent(shapeIdA: b2ShapeId?, shapeIdB: b2ShapeId?) : PhysicsEvent(shapeIdA, shapeIdB)

  /**
   * Material of block changed
   */
  class BlockRemovedEvent(shapeIdA: b2ShapeId?, val compactLocation: Long) : PhysicsEvent(shapeIdA, null)

  companion object {
    fun findBodyUserData(shapeId: b2ShapeId?): Any? {
      if (shapeId == null || !shapeId.isValid) return null
      val body = shapeId.body
      if (!body.isValid) return null
      return body.userData
    }

    fun findShapeUserData(shapeId: b2ShapeId?): Any? = if (shapeId != null && shapeId.isValid) shapeId.userData else null
  }
}
