package no.elg.infiniteBootleg.server.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import no.elg.infiniteBootleg.core.net.clientBoundMoveEntity
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_LAST
import no.elg.infiniteBootleg.core.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.EFFECTIVE_ZERO
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityComponent
import no.elg.infiniteBootleg.core.world.ecs.system.api.ConditionalIteratingSystem
import no.elg.infiniteBootleg.server.ServerMain

object BroadcastMovingPlayerPositions : ConditionalIteratingSystem(basicDynamicEntityFamily, UPDATE_PRIORITY_LAST) {

  private val vel = Vector2()

  override fun condition(entity: Entity): Boolean {
    entity.velocityComponent.toVector2(vel)
    return !vel.isZero(EFFECTIVE_ZERO)
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    ServerMain.inst().packetSender.broadcastToInView(clientBoundMoveEntity(entity), entity, excludeEntity = true)
  }
}
