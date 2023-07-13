package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.physics.box2d.Body
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_FIRST
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.world.ecs.components.LookDirectionComponent.Companion.lookDirectionOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocityOrNull
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.UpdateBox2DPositionTag.Companion.updateBox2DPosition
import no.elg.infiniteBootleg.world.ecs.components.tags.UpdateBox2DVelocityTag.Companion.updateBox2DVelocity
import kotlin.math.abs

/**
 * Read the position of the entity from the box2D entity
 */
object ReadBox2DStateSystem : IteratingSystem(basicDynamicEntityFamily, UPDATE_PRIORITY_FIRST) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val body = entity.box2dBody
    readPosition(entity, body)
    readVelocity(entity, body)
  }

  private fun readPosition(entity: Entity, body: Body) {
    if (!entity.updateBox2DPosition) {
      entity.positionComponent.x = body.position.x
      entity.positionComponent.y = body.position.y
    }
  }

  private fun readVelocity(entity: Entity, body: Body) {
    val velocity = entity.velocityOrNull ?: return
    if (!entity.updateBox2DVelocity) {
      velocity.dx = body.linearVelocity.x
      velocity.dy = body.linearVelocity.y
    }
    val lookDirection = entity.lookDirectionOrNull ?: return
    if (abs(velocity.dx) > 0.2f) {
      lookDirection.direction = if (velocity.dx < 0f) Direction.WEST else Direction.EAST
    }
  }
}
