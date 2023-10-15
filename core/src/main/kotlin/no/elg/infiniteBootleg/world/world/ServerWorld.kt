package no.elg.infiniteBootleg.world.world

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.server.broadcastToInView
import no.elg.infiniteBootleg.server.clientBoundSpawnEntity
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.required.EntityTypeComponent.Companion.entityTypeComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.shouldSendToClients
import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.world.render.HeadlessWorldRenderer
import no.elg.infiniteBootleg.world.render.ServerClientChunksInView

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
      Main.logger().warn("SERVER", "Failed to find player $uuid to remove")
    }
  }

  override fun addEntityListeners(engine: Engine) {
    engine.addEntityListener(
      basicDynamicEntityFamily,
      object : EntityListener {
        fun isPlayer(entity: Entity) = entity.entityTypeComponent.entityType == ProtoWorld.Entity.EntityType.PLAYER
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
    render.addClient(player.id, ServerClientChunksInView(player.positionComponent.x.worldToChunk(), player.positionComponent.y.worldToChunk()))
    render.update()
    if (player.shouldSendToClients) {
      Main.inst().scheduler.executeSync {
        broadcastToInView(
          clientBoundSpawnEntity(player),
          player.getComponent(PositionComponent::class.java).blockX,
          player.getComponent(PositionComponent::class.java).blockY
        )
      }
    }
  }

  private fun onEntityRemove(player: Entity) {
    render.removeClient(player.id)
  }
}
