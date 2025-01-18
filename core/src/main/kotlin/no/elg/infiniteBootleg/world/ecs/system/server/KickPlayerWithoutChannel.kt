package no.elg.infiniteBootleg.world.ecs.system.server

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.main.ServerMain
import no.elg.infiniteBootleg.server.ServerBoundHandler.Companion.clients
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_LAST
import no.elg.infiniteBootleg.world.ecs.api.restriction.system.ServerSystem
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.playerFamily

private val logger = KotlinLogging.logger {}

object KickPlayerWithoutChannel : IteratingSystem(playerFamily, UPDATE_PRIORITY_LAST), ServerSystem {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val uuid = entity.id
    if (clients.values.none { it.entityId == uuid }) {
      logger.warn { "Found an unknown player $uuid (name: ${entity.nameOrNull}), disconnecting" }
      ServerMain.inst().serverWorld.disconnectPlayer(uuid, false)
    }
  }
}
