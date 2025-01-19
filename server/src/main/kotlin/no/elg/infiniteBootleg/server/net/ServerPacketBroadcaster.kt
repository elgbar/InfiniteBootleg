package no.elg.infiniteBootleg.server.net

import com.badlogic.ashley.core.Entity
import io.netty.channel.group.ChannelMatcher
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.server.PacketBroadcaster
import no.elg.infiniteBootleg.server.world.ServerWorld
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent

class ServerPacketBroadcaster(val world: ServerWorld) : PacketBroadcaster {

  /**
   * Broadcast a packet to players which have the given [worldPosition] location loaded.
   *
   * Can only be used by a server instance
   */
  override fun broadcastToInViewChunk(packet: Packets.Packet, chunkX: ChunkCoord, chunkY: ChunkCoord, filter: ChannelMatcher) {
    broadcast(packet) { channel ->
      val sharedInfo = ServerBoundHandler.clients[channel] ?: return@broadcast false
      val viewing = world.render.getClient(sharedInfo.entityId) ?: return@broadcast false
      return@broadcast viewing.isInView(chunkX, chunkY) && filter.matches(channel)
    }
  }

  override fun broadcastToInView(packet: Packets.Packet, worldX: WorldCoord, worldY: WorldCoord, filter: ChannelMatcher) {
    broadcastToInViewChunk(packet, worldX.worldToChunk(), worldY.worldToChunk(), filter)
  }

  override fun broadcastToInView(packet: Packets.Packet, entity: Entity, filter: ChannelMatcher) {
    val (worldX, worldY) = entity.positionComponent
    broadcastToInViewChunk(packet, worldX.worldToChunk(), worldY.worldToChunk(), filter)
  }

  /**
   * Broadcast to all other channels than [this]
   */
  override fun broadcast(packet: Packets.Packet, filter: ChannelMatcher) {
    ServerBoundHandler.Companion.channels.writeAndFlush(packet, filter)
  }
}
