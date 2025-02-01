package no.elg.infiniteBootleg.server.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.server.ServerMain
import no.elg.infiniteBootleg.server.net.ServerBoundHandler
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_LAST
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.playerFamily

private val logger = KotlinLogging.logger {}

object KickPlayerWithoutChannel : IteratingSystem(playerFamily, UPDATE_PRIORITY_LAST) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val entityId = entity.id
    if (ServerBoundHandler.Companion.clients.values.none { it.entityId == entityId }) {
      logger.warn { "Found an unknown player $entityId (name: ${entity.nameOrNull}), disconnecting" }
      ServerMain.Companion.inst().serverWorld.disconnectPlayer(entityId, false)
    }
  }
}
