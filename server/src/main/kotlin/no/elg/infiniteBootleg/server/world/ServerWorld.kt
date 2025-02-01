package no.elg.infiniteBootleg.server.world

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.EntitySystem
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.launchOnMain
import no.elg.infiniteBootleg.core.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.core.world.ticker.WorldTicker
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.net.clientBoundSpawnEntity
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.server.net.despawnEntity
import no.elg.infiniteBootleg.server.world.ecs.system.KickPlayerWithoutChannel
import no.elg.infiniteBootleg.server.world.loader.ServerWorldLoader
import no.elg.infiniteBootleg.server.world.render.HeadlessWorldRenderer
import no.elg.infiniteBootleg.server.world.ticker.ServerWorldTicker
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.required.EntityTypeComponent.Companion.isType
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.shouldSendToClients
import no.elg.infiniteBootleg.world.render.ServerClientChunksInView

private val logger = KotlinLogging.logger {}

/**
 * World with extra functionality to handle multiple players
 *
 * @author Elg
 */
class ServerWorld(generator: ChunkGenerator, seed: Long, worldName: String) : World(generator, seed, worldName) {

  override val render = HeadlessWorldRenderer(this)
  override val worldTicker: WorldTicker = ServerWorldTicker(this, tick = false)

  fun disconnectPlayer(entityId: String, kicked: Boolean) {
    val player = getEntity(entityId)
    if (player != null) {
      removeEntity(player, if (kicked) Packets.DespawnEntity.DespawnReason.PLAYER_KICKED else Packets.DespawnEntity.DespawnReason.PLAYER_QUIT)
    } else {
      logger.warn { "Failed to find player $entityId to remove" }
    }
  }

  override fun additionalSystems(): Set<EntitySystem> = setOf(KickPlayerWithoutChannel)

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
    val positionComponent = player.positionComponent
    val chunkX = positionComponent.x.worldToChunk()
    val chunkY = positionComponent.y.worldToChunk()
    render.addClient(player.id, ServerClientChunksInView(chunkX, chunkY))
    render.update()
    if (player.shouldSendToClients) {
      launchOnMain {
        Main.Companion.inst().packetSender.broadcastToInViewChunk(clientBoundSpawnEntity(player), chunkX, chunkY)
      }
    }
  }

  private fun onEntityRemove(player: Entity) {
    ServerWorldLoader.saveServerPlayer(player)
    render.removeClient(player.id)
  }

  override fun removeEntity(entity: Entity, reason: Packets.DespawnEntity.DespawnReason) {
    despawnEntity(entity, reason)
    super.removeEntity(entity, reason)
  }

  override fun save() {
    playersEntities.forEach(ServerWorldLoader::saveServerPlayer)
    super.save()
  }
}
