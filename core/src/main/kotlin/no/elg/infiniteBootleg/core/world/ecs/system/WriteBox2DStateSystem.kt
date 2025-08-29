package no.elg.infiniteBootleg.core.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.box2d.structs.b2BodyId
import no.elg.infiniteBootleg.core.world.box2d.extensions.isAwake
import no.elg.infiniteBootleg.core.world.box2d.extensions.makeB2Vec2
import no.elg.infiniteBootleg.core.world.box2d.extensions.position
import no.elg.infiniteBootleg.core.world.box2d.extensions.velocity
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_LAST
import no.elg.infiniteBootleg.core.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.core.world.ecs.components.GroundedComponent.Companion.groundedComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.UpdateBox2DPositionTag.Companion.updateBox2DPosition
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.UpdateBox2DVelocityTag.Companion.updateBox2DVelocity
import no.elg.infiniteBootleg.core.world.ecs.system.api.AuthorizedEntitiesIteratingSystem

/**
 * Write the position of the entity from the box2D entity
 */
object WriteBox2DStateSystem : AuthorizedEntitiesIteratingSystem(basicDynamicEntityFamily, UPDATE_PRIORITY_LAST) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val body = entity.box2dBody
    updatePosition(entity, body)
    updateVelocity(entity, body)
  }

  private fun updateVelocity(entity: Entity, body: b2BodyId) {
    if (entity.updateBox2DVelocity) {
      entity.updateBox2DVelocity = false
      updateVelocity(body, entity.velocityComponent.dx, entity.velocityComponent.dy)
    }
  }

  private fun updatePosition(entity: Entity, body: b2BodyId) {
    if (entity.updateBox2DPosition) {
      entity.updateBox2DPosition = false

      entity.groundedComponentOrNull?.clearContacts()

      val posComp = entity.positionComponent
      updatePosition(body, posComp.x, posComp.y)
    }
  }

  /**
   * Official way to transfer the velocity of an entity to the box2d body
   *
   * MUST BE CALLED ON BOX2D THREAD!
   */
  fun updateVelocity(body: b2BodyId, dx: Float, dy: Float) {
    body.velocity = makeB2Vec2(dx, dy)
  }

  /**
   * Official way to transfer the position of an entity to the box2d body
   *
   * MUST BE CALLED ON BOX2D THREAD!
   */
  fun updatePosition(body: b2BodyId, x: Float, y: Float) {
    body.position = makeB2Vec2(x, y)
    body.isAwake = true
  }
}
