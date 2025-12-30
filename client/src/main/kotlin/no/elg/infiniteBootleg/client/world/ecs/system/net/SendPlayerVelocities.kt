package no.elg.infiniteBootleg.client.world.ecs.system.net

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IntervalIteratingSystem
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.ecs.system.net.SendPlayerVelocities.UPDATE_INTERVAL_SECONDS
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.net.ServerClient.Companion.sendServerBoundPacket
import no.elg.infiniteBootleg.core.net.serverBoundMoveEntityPacket
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_LAST
import no.elg.infiniteBootleg.core.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityComponent

object SendPlayerVelocities : IntervalIteratingSystem(basicDynamicEntityFamily, UPDATE_INTERVAL_SECONDS, UPDATE_PRIORITY_LAST) {

  const val UPDATE_INTERVAL_SECONDS = 0.1f

  override fun processEntity(entity: Entity) {
    if (Main.inst().isAuthorizedToChange(entity)) {
      val velocityComponent = entity.velocityComponent
      if (velocityComponent.isMoving()) {
        ClientMain.inst().serverClient?.sendServerBoundPacket {
          serverBoundMoveEntityPacket(entity)
        }
      }
    }
  }
}
