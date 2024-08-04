package no.elg.infiniteBootleg.world.world

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.server.broadcastToInViewChunk
import no.elg.infiniteBootleg.server.clientBoundSpawnEntity
import no.elg.infiniteBootleg.util.launchOnMain
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.required.EntityTypeComponent.Companion.isType
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.shouldSendToClients
import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.world.loader.WorldLoader
import no.elg.infiniteBootleg.world.render.HeadlessWorldRenderer
import no.elg.infiniteBootleg.world.render.ServerClientChunksInView

private val logger = KotlinLogging.logger {}

/**
 * World with extra functionality to handle multiple players
 *
 * @author Elg
 */
class ServerWorld(generator: ChunkGenerator, seed: Long, worldName: String) : World(generator, seed, worldName) {

  override val render = HeadlessWorldRenderer(this)

  fun disconnectPlayer(uuid: String, kicked: Boolean) {
    val player = getEntity(uuid)
    if (player != null) {
      removeEntity(player, if (kicked) DespawnReason.PLAYER_KICKED else DespawnReason.PLAYER_QUIT)
    } else {
      logger.warn { "Failed to find player $uuid to remove" }
    }
  }

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
        broadcastToInViewChunk(clientBoundSpawnEntity(player), chunkX, chunkY)
      }
    }
  }

  private fun onEntityRemove(player: Entity) {
    WorldLoader.saveServerPlayer(player)
    render.removeClient(player.id)
  }
}
