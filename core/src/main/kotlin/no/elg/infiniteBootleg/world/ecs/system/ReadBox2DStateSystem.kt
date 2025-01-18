package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.physics.box2d.Body
import ktx.math.component1
import ktx.math.component2
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.server.ServerClient.Companion.sendServerBoundPacket
import no.elg.infiniteBootleg.server.broadcastToInView
import no.elg.infiniteBootleg.server.clientBoundMoveEntity
import no.elg.infiniteBootleg.server.serverBoundMoveEntityPacket
import no.elg.infiniteBootleg.util.launchOnAsync
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_BEFORE_EVENTS
import no.elg.infiniteBootleg.world.ecs.api.restriction.system.UniversalSystem
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocityOrZero
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.tags.UpdateBox2DPositionTag.Companion.updateBox2DPosition
import no.elg.infiniteBootleg.world.ecs.components.transients.tags.UpdateBox2DVelocityTag.Companion.updateBox2DVelocity
import kotlin.math.abs

/**
 * Read the position of the entity from the box2D entity
 */
object ReadBox2DStateSystem : IteratingSystem(basicDynamicEntityFamily, UPDATE_PRIORITY_BEFORE_EVENTS), UniversalSystem {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val body = entity.box2dBody

    val updatePos = readPosition(entity, body)
    val updateVel = readVelocity(entity, body)

    if (updatePos || updateVel) {
      if (Main.isServerClient) {
        ClientMain.inst().serverClient?.let { serverClient ->
          if (serverClient.entityId == entity.id) {
            launchOnAsync {
              serverClient.sendServerBoundPacket { serverBoundMoveEntityPacket(entity) }
            }
          }
        }
      } else if (Main.isServer) {
        broadcastToInView(clientBoundMoveEntity(entity), body.position.x.toInt(), body.position.x.toInt())
      }
    }
  }

  private fun readPosition(entity: Entity, body: Body): Boolean {
    if (!entity.updateBox2DPosition) {
      val updateServer = if (Main.isMultiplayer) {
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

    val updateServer = if (Main.isMultiplayer) {
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
    if (abs(newDx) > MIN_VELOCITY_TO_FLIP && Main.inst().isAuthorizedToChange(entity)) {
      lookDirection.direction = if (newDx < 0f) Direction.WEST else Direction.EAST
    }
    return updateServer
  }

  private const val MIN_VELOCITY_TO_FLIP = 0.2f
  private const val POSITION_SQUARED_DIFF_TO_SEND_ENTITY_MOVE_PACKET = 0.15f
  private const val VELOCITY_SQUARED_DIFF_TO_SEND_ENTITY_MOVE_PACKET = 0.1f
}
