package no.elg.infiniteBootleg.server.world.ecs.system

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.core.net.clientBoundMoveEntity
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_LAST
import no.elg.infiniteBootleg.core.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityComponent
import no.elg.infiniteBootleg.core.world.ecs.system.api.ConditionalIteratingSystem
import no.elg.infiniteBootleg.server.ServerMain

object BroadcastMovingPlayerPositions : ConditionalIteratingSystem(basicDynamicEntityFamily, UPDATE_PRIORITY_LAST) {

  override fun condition(entity: Entity): Boolean = entity.velocityComponent.isMoving()

  override fun processEntity(entity: Entity, deltaTime: Float) {
    ServerMain.inst().packetSender.broadcastToInView(clientBoundMoveEntity(entity), entity, excludeEntity = true)
  }
}
