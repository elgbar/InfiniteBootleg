package no.elg.infiniteBootleg.server.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_LAST
import no.elg.infiniteBootleg.core.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.ecs.playerFamily
import no.elg.infiniteBootleg.server.ServerMain
import no.elg.infiniteBootleg.server.net.ServerBoundHandler

private val logger = KotlinLogging.logger {}

object KickPlayerWithoutChannel : IteratingSystem(playerFamily, UPDATE_PRIORITY_LAST) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val entityId = entity.id
    if (ServerBoundHandler.clients.values.none { it.entityId == entityId }) {
      logger.warn { "Found an unknown player $entityId (name: ${entity.nameOrNull}), disconnecting" }
      ServerMain.inst().serverWorld.disconnectPlayer(entityId, false)
    }
  }
}
