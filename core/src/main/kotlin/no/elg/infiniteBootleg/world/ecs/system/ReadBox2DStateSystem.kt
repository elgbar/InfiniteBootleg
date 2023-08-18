package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.physics.box2d.Body
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_BEFORE_EVENTS
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.tags.UpdateBox2DPositionTag.Companion.updateBox2DPosition
import no.elg.infiniteBootleg.world.ecs.components.transients.tags.UpdateBox2DVelocityTag.Companion.updateBox2DVelocity
import kotlin.math.abs

/**
 * Read the position of the entity from the box2D entity
 */
object ReadBox2DStateSystem : IteratingSystem(basicDynamicEntityFamily, UPDATE_PRIORITY_BEFORE_EVENTS) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val body = entity.box2dBody
    readPosition(entity, body)
    readVelocity(entity, body)
  }

  private fun readPosition(entity: Entity, body: Body) {
    if (!entity.updateBox2DPosition) {
      entity.positionComponent.setPosition(body.position)
    }
  }

  private fun readVelocity(entity: Entity, body: Body) {
    val newDx = body.linearVelocity.x
    if (!entity.updateBox2DVelocity) {
      entity.setVelocity(newDx, body.linearVelocity.y)
      entity.updateBox2DVelocity = false
    }
    val lookDirection = entity.lookDirectionComponentOrNull ?: return
    if (abs(newDx) > MIN_VELOCITY_TO_FLIP) {
      lookDirection.direction = if (newDx < 0f) Direction.WEST else Direction.EAST
    }
  }

  private const val MIN_VELOCITY_TO_FLIP = 0.2f
}
