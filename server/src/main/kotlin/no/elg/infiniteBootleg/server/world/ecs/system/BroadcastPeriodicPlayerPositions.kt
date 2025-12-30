package no.elg.infiniteBootleg.server.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IntervalIteratingSystem
import no.elg.infiniteBootleg.core.net.clientBoundMoveEntity
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_LAST
import no.elg.infiniteBootleg.core.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.server.ServerMain

object BroadcastPeriodicPlayerPositions : IntervalIteratingSystem(basicDynamicEntityFamily, 1f, UPDATE_PRIORITY_LAST) {

  override fun processEntity(entity: Entity) {
    ServerMain.inst().packetSender.broadcastToInView(clientBoundMoveEntity(entity), entity, excludeEntity = false)
  }
}
