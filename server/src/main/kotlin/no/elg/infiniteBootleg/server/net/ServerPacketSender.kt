package no.elg.infiniteBootleg.server.net

import com.badlogic.ashley.core.Entity
import io.netty.channel.Channel
import io.netty.channel.group.ChannelMatcher
import io.netty.channel.group.ChannelMatchers
import no.elg.infiniteBootleg.core.net.PacketSender
import no.elg.infiniteBootleg.core.net.ServerClient
import no.elg.infiniteBootleg.core.util.ChunkCompactLoc
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.decompactLocX
import no.elg.infiniteBootleg.core.util.decompactLocY
import no.elg.infiniteBootleg.core.util.worldToChunk
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.protobuf.Packets.Packet
import no.elg.infiniteBootleg.server.world.ServerWorld
import no.elg.infiniteBootleg.server.world.ecs.components.transients.ServerClientChunksInViewComponent.Companion.chunksInView

class ServerPacketSender(private val world: ServerWorld) : PacketSender {

  /**
   * Broadcast a packet to players which have the given [worldPosition] location loaded.

   */
  fun broadcastToInViewChunk(packet: Packet, chunkX: ChunkCoord, chunkY: ChunkCoord, filter: ChannelMatcher = ChannelMatchers.all()) {
    broadcast(packet) { channel ->
      val player = world.getPlayer(channel) ?: return@broadcast false
      return@broadcast player.chunksInView.isInView(chunkX, chunkY) && filter.matches(channel)
    }
  }

  /**
   * Broadcast a packet to players which have the given [worldPosition] location loaded.

   */
  fun broadcastToInView(packet: Packet, worldX: WorldCoord, worldY: WorldCoord, filter: ChannelMatcher = ChannelMatchers.all()) {
    broadcastToInViewChunk(packet, worldX.worldToChunk(), worldY.worldToChunk(), filter)
  }

  fun entityFilter(entityId: String): ChannelMatcher = ChannelMatcher { channel: Channel -> ServerBoundHandler.clients[channel]?.entityId != entityId }

  /**
   * Broadcast a packet to players which have the chunks loaded where the entity is located
   *
   * @param packet the packet to broadcast to clients
   * @param entity the entity to use as the location to broadcast the packet from
   * @param excludeEntity if true the [entity] will not receive the packet
   */
  fun broadcastToInView(packet: Packet, entity: Entity, excludeEntity: Boolean) {
    val (worldX, worldY) = entity.positionComponent
    val filter: ChannelMatcher = if (excludeEntity) entityFilter(entity.id) else ChannelMatchers.all()
    broadcastToInViewChunk(packet, worldX.worldToChunk(), worldY.worldToChunk(), filter)
  }

  fun broadcast(packet: Packet, filter: ChannelMatcher = ChannelMatchers.all()) {
    ServerBoundHandler.channels.writeAndFlush(packet, filter)
  }

  @Deprecated("Use sendDuplexPacketInView instead when on the server")
  override fun sendDuplexPacket(ifIsServer: () -> Packet, ifIsClient: ServerClient.() -> Packet) {
    broadcast(ifIsServer())
  }

  @Deprecated("Use broadcastToInViewChunk instead when on the server")
  override fun sendDuplexPacketInView(ifIsServer: () -> Pair<Packet?, ChunkCompactLoc>, ifIsClient: ServerClient.() -> Packet?) {
    val (maybePacket, chunkLoc) = ifIsServer()
    val packet = maybePacket ?: return
    broadcastToInViewChunk(packet, chunkLoc.decompactLocX(), chunkLoc.decompactLocY())
  }
}
