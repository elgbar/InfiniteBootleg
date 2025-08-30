package no.elg.infiniteBootleg.core.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.box2d.structs.b2BodyId
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.core.world.box2d.extensions.component1
import no.elg.infiniteBootleg.core.world.box2d.extensions.component2
import no.elg.infiniteBootleg.core.world.box2d.extensions.position
import no.elg.infiniteBootleg.core.world.box2d.extensions.velocity
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_BEFORE_EVENTS
import no.elg.infiniteBootleg.core.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.core.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import kotlin.math.abs

/**
 * Read the position of the entity from **dynamic** the box2D entity
 *
 * We do not read or update entities without the [no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent], as they should never be moved once placed.
 * They may also a difference in box2d and ashley position.
 */
object ReadBox2DStateSystem : IteratingSystem(basicDynamicEntityFamily, UPDATE_PRIORITY_BEFORE_EVENTS) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val body = entity.box2dBody

    readPosition(entity, body)
    readVelocity(entity, body)
  }

  private fun readPosition(entity: Entity, body: b2BodyId) {
    val newPosition = body.position
    entity.positionComponent.setPosition(newPosition)
  }

  private fun readVelocity(entity: Entity, body: b2BodyId) {
    val (newDx, newDy) = body.velocity
    entity.velocityComponent.setAshleyVelocity(newDx, newDy)

    val lookDirection = entity.lookDirectionComponentOrNull ?: return
    if (abs(newDx) > MIN_VELOCITY_TO_FLIP && Main.inst().isAuthorizedToChange(entity)) {
      lookDirection.direction = if (newDx < 0f) Direction.WEST else Direction.EAST
    }
  }

  private const val MIN_VELOCITY_TO_FLIP = 0.2f
}
