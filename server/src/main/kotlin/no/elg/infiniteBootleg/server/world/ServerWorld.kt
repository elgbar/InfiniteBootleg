package no.elg.infiniteBootleg.server.world

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.EntitySystem
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.Channel
import no.elg.infiniteBootleg.core.net.SharedInformation
import no.elg.infiniteBootleg.core.net.clientBoundSpawnEntity
import no.elg.infiniteBootleg.core.util.IllegalAction
import no.elg.infiniteBootleg.core.util.launchOnMainSuspendable
import no.elg.infiniteBootleg.core.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.components.required.EntityTypeComponent.Companion.isType
import no.elg.infiniteBootleg.core.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.shouldSendToClients
import no.elg.infiniteBootleg.core.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.core.world.loader.WorldLoader
import no.elg.infiniteBootleg.core.world.ticker.WorldTicker
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.server.ServerMain
import no.elg.infiniteBootleg.server.net.ServerBoundHandler
import no.elg.infiniteBootleg.server.net.despawnEntity
import no.elg.infiniteBootleg.server.world.ecs.system.BroadcastMovingPlayerPositions
import no.elg.infiniteBootleg.server.world.ecs.system.BroadcastPeriodicPlayerPositions
import no.elg.infiniteBootleg.server.world.ecs.system.CenterViewOnEntity
import no.elg.infiniteBootleg.server.world.ecs.system.KickPlayerWithoutChannel
import no.elg.infiniteBootleg.server.world.loader.ServerWorldLoader
import no.elg.infiniteBootleg.server.world.render.HeadlessWorldRenderer
import no.elg.infiniteBootleg.server.world.ticker.ServerWorldTicker

private val logger = KotlinLogging.logger {}

/**
 * World with extra functionality to handle multiple players
 *
 * @author Elg
 */
class ServerWorld(generator: ChunkGenerator, seed: Long, worldName: String) : World(generator, seed, worldName) {

  override val render = HeadlessWorldRenderer(this)
  override val worldTicker: WorldTicker = ServerWorldTicker(this, tick = false)

  override fun initialize() {
    super.initialize()
    if (isTransient) {
      IllegalAction.CRASH.handle {
        val worldLockFile = WorldLoader.getWorldLockFile(uuid)
        "Server is marked as transient which is not allowed. " +
          "You may have to manually delete the world lock file located at '$worldLockFile' if there is no process with the id in th lock file"
      }
    }
  }

  fun disconnectPlayer(entityId: String, kicked: Boolean) {
    val player = getEntity(entityId)
    if (player != null) {
      removeEntity(player, if (kicked) Packets.DespawnEntity.DespawnReason.PLAYER_KICKED else Packets.DespawnEntity.DespawnReason.PLAYER_QUIT)
    } else {
      logger.warn { "Failed to find player $entityId to remove" }
    }
  }

  override fun additionalSystems(): Set<EntitySystem> =
    setOf(
      KickPlayerWithoutChannel,
      BroadcastMovingPlayerPositions,
      BroadcastPeriodicPlayerPositions,
      CenterViewOnEntity
    )

  override fun addEntityListeners(engine: Engine) {
    engine.addEntityListener(
      basicDynamicEntityFamily,
      object : EntityListener {
        fun isPlayer(entity: Entity) = entity.isType(ProtoWorld.Entity.EntityType.PLAYER)
        override fun entityAdded(entity: Entity) {
          if (isPlayer(entity)) onEntityAdd(entity)
        }

        override fun entityRemoved(entity: Entity) {
          if (isPlayer(entity)) onEntityRemove(entity)
        }
      }
    )
  }

  private fun onEntityAdd(player: Entity) {
    if (player.shouldSendToClients) {
      launchOnMainSuspendable {
        ServerMain.inst().packetSender.broadcastToInView(clientBoundSpawnEntity(player), player, excludeEntity = false)
      }
    }
  }

  private fun onEntityRemove(player: Entity) {
    ServerWorldLoader.saveServerPlayer(player)
  }

  override fun removeEntity(entity: Entity, reason: Packets.DespawnEntity.DespawnReason) {
    despawnEntity(entity, reason)
    super.removeEntity(entity, reason)
  }

  override fun save() {
    playersEntities.forEach(ServerWorldLoader::saveServerPlayer)
    super.save()
  }

  fun getPlayer(channel: Channel): Entity? {
    val information = ServerBoundHandler.clients[channel] ?: return null
    return getPlayer(information)
  }

  fun getPlayer(sharedInformation: SharedInformation): Entity? = getEntity(sharedInformation.entityId)
}
