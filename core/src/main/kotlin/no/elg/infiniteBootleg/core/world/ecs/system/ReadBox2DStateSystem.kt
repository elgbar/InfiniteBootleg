package no.elg.infiniteBootleg.core.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.physics.box2d.Body
import ktx.math.component1
import ktx.math.component2
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_BEFORE_EVENTS
import no.elg.infiniteBootleg.core.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.core.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.UpdateBox2DPositionTag.Companion.updateBox2DPosition
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.UpdateBox2DVelocityTag.Companion.updateBox2DVelocity
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
      val newPosition = body.position
      entity.positionComponent.setPosition(newPosition)
    }
  }

  private fun readVelocity(entity: Entity, body: Body) {
    val newVelocity = body.linearVelocity
    val (newDx, newDy) = newVelocity

    if (!entity.updateBox2DVelocity) {
      entity.setVelocity(newDx, newDy)
      entity.updateBox2DVelocity = false
    }

    val lookDirection = entity.lookDirectionComponentOrNull ?: return
    if (abs(newDx) > MIN_VELOCITY_TO_FLIP && Main.inst().isAuthorizedToChange(entity)) {
      lookDirection.direction = if (newDx < 0f) Direction.WEST else Direction.EAST
    }
  }

  private const val MIN_VELOCITY_TO_FLIP = 0.2f
}
