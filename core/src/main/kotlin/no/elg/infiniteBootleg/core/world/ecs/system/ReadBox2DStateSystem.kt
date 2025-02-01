package no.elg.infiniteBootleg.core.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.physics.box2d.Body
import ktx.math.component1
import ktx.math.component2
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.net.clientBoundMoveEntity
import no.elg.infiniteBootleg.core.net.serverBoundMoveEntityPacket
import no.elg.infiniteBootleg.core.util.toCompactLoc
import no.elg.infiniteBootleg.core.util.worldToChunk
import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_BEFORE_EVENTS
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.system.UniversalSystem
import no.elg.infiniteBootleg.core.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.core.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityOrZero
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.UpdateBox2DPositionTag.Companion.updateBox2DPosition
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.UpdateBox2DVelocityTag.Companion.updateBox2DVelocity
import kotlin.math.abs

/**
 * Read the position of the entity from the box2D entity
 */
object ReadBox2DStateSystem :
  IteratingSystem(basicDynamicEntityFamily, UPDATE_PRIORITY_BEFORE_EVENTS),
  UniversalSystem {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val body = entity.box2dBody

    val updatePos = readPosition(entity, body)
    val updateVel = readVelocity(entity, body)

    if (updatePos || updateVel) {
      Main.Companion.inst().packetSender.sendDuplexPacketInView(ifIsServer = { clientBoundMoveEntity(entity) to body.position.toCompactLoc().worldToChunk() }, ifIsClient = {
        if (entityId == entity.id) {
          serverBoundMoveEntityPacket(entity)
        } else {
          null
        }
      })
    }
  }

  private fun readPosition(entity: Entity, body: Body): Boolean {
    if (!entity.updateBox2DPosition) {
      val updateServer = if (Main.Companion.isMultiplayer) {
        val oldPosition = entity.position
        val newPosition = body.position
        oldPosition.dst2(newPosition) > POSITION_SQUARED_DIFF_TO_SEND_ENTITY_MOVE_PACKET
      } else {
        false
      }
      entity.positionComponent.setPosition(body.position)
      return updateServer
    }
    return false
  }

  private fun readVelocity(entity: Entity, body: Body): Boolean {
    val newVelocity = body.linearVelocity
    val (newDx, newDy) = newVelocity

    val updateServer = if (Main.Companion.isMultiplayer) {
      val oldVelocity = entity.velocityOrZero
      oldVelocity.dst2(newVelocity) > VELOCITY_SQUARED_DIFF_TO_SEND_ENTITY_MOVE_PACKET
    } else {
      false
    }

    if (!entity.updateBox2DVelocity) {
      entity.setVelocity(newDx, newDy)
      entity.updateBox2DVelocity = false
    }

    val lookDirection = entity.lookDirectionComponentOrNull ?: return updateServer
    if (abs(newDx) > MIN_VELOCITY_TO_FLIP && Main.Companion.inst().isAuthorizedToChange(entity)) {
      lookDirection.direction = if (newDx < 0f) Direction.WEST else Direction.EAST
    }
    return updateServer
  }

  private const val MIN_VELOCITY_TO_FLIP = 0.2f
  private const val POSITION_SQUARED_DIFF_TO_SEND_ENTITY_MOVE_PACKET = 0.15f
  private const val VELOCITY_SQUARED_DIFF_TO_SEND_ENTITY_MOVE_PACKET = 0.1f
}
