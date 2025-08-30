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
import no.elg.infiniteBootleg.core.world.ecs.basicStandaloneEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.core.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import kotlin.math.abs

/**
 * Read the position of the entity from the box2D entity
 */
object ReadBox2DStateSystem : IteratingSystem(basicStandaloneEntityFamily, UPDATE_PRIORITY_BEFORE_EVENTS) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val body = entity.box2dBody

    readPosition(entity, body)
    entity.velocityComponentOrNull?.also { velComp ->
      readVelocity(entity, body, velComp)
    }
  }

  private fun readPosition(entity: Entity, body: b2BodyId) {
    val newPosition = body.position
    entity.positionComponent.setPosition(newPosition)
  }

  private fun readVelocity(entity: Entity, body: b2BodyId, velComp: VelocityComponent) {
    val (newDx, newDy) = body.velocity
    velComp.setAshleyVelocity(newDx, newDy)

    val lookDirection = entity.lookDirectionComponentOrNull ?: return
    if (abs(newDx) > MIN_VELOCITY_TO_FLIP && Main.inst().isAuthorizedToChange(entity)) {
      lookDirection.direction = if (newDx < 0f) Direction.WEST else Direction.EAST
    }
  }

  private const val MIN_VELOCITY_TO_FLIP = 0.2f
}
