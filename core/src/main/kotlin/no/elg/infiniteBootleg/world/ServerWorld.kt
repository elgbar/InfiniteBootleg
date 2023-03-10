package no.elg.infiniteBootleg.world

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.server.broadcastToInView
import no.elg.infiniteBootleg.server.clientBoundSpawnEntity
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.playerFamily
import no.elg.infiniteBootleg.world.generator.ChunkGenerator
import no.elg.infiniteBootleg.world.render.HeadlessWorldRenderer
import no.elg.infiniteBootleg.world.render.ServerClientChunksInView

/**
 * World with extra functionality to handle multiple players
 *
 * @author Elg
 */
class ServerWorld : World {
  override val render = HeadlessWorldRenderer(this)

  constructor(protoWorld: ProtoWorld.World) : super(protoWorld)
  constructor(generator: ChunkGenerator, seed: Long, worldName: String) : super(generator, seed, worldName)

  fun disconnectPlayer(uuid: String, kicked: Boolean) {
    val player = getEntity(uuid)
    if (player != null) {
      removeEntity(player, if (kicked) DespawnReason.PLAYER_KICKED else DespawnReason.PLAYER_QUIT)
    } else {
      Main.logger().warn("Failed to find player $uuid to remove")
    }
  }

  override fun initializeEngine(): Engine = super.initializeEngine().also {
    it.addEntityListener(object : EntityListener {
      override fun entityAdded(entity: Entity): Unit = onEntityAdd(entity)
      override fun entityRemoved(entity: Entity): Unit = onEntityRemove(entity)
    })
  }

  private fun onEntityAdd(entity: Entity) {
    if (playerFamily.matches(entity)) {
      render.addClient(entity.id, ServerClientChunksInView(entity.positionComponent.x.worldToChunk(), entity.positionComponent.y.worldToChunk()))
    }
    render.update()
    Main.inst().scheduler.executeSync {
      broadcastToInView(
        clientBoundSpawnEntity(entity),
        entity.getComponent(PositionComponent::class.java).blockX,
        entity.getComponent(PositionComponent::class.java).blockY,
        null
      )
    }
  }

  private fun onEntityRemove(entity: Entity) {
    if (playerFamily.matches(entity)) {
      render.removeClient(entity.id)
    }
  }
}
