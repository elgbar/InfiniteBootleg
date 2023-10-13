package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_LAST
import no.elg.infiniteBootleg.world.ecs.api.restriction.DuplexSystem
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent.Companion.groundedComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocityComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.tags.UpdateBox2DPositionTag.Companion.updateBox2DPosition
import no.elg.infiniteBootleg.world.ecs.components.transients.tags.UpdateBox2DVelocityTag.Companion.updateBox2DVelocity

/**
 * Write the position of the entity from the box2D entity
 */
object WriteBox2DStateSystem : IteratingSystem(basicDynamicEntityFamily, UPDATE_PRIORITY_LAST), DuplexSystem {

  private val tmp = Vector2()

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val body = entity.box2dBody
    updatePosition(entity, body)
    updateVelocity(entity, body)
  }

  private fun updateVelocity(entity: Entity, body: Body) {
    if (entity.updateBox2DVelocity) {
      entity.updateBox2DVelocity = false
      body.setLinearVelocity(entity.velocityComponent.dx, entity.velocityComponent.dy)
    }
  }

  private fun updatePosition(entity: Entity, body: Body) {
    if (entity.updateBox2DPosition) {
      entity.updateBox2DPosition = false

      entity.groundedComponentOrNull?.clearContacts()

      tmp.x = entity.positionComponent.x
      tmp.y = entity.positionComponent.y
      body.setTransform(tmp, 0f)
      body.isAwake = true
    }
  }
}
