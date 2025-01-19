package no.elg.infiniteBootleg.server

import com.badlogic.ashley.core.Entity
import io.netty.channel.group.ChannelMatcher
import io.netty.channel.group.ChannelMatchers
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.singleLinePrinter

/**
 * Broadcast packets to all clients, does nothing on client
 */
interface PacketBroadcaster {
  fun broadcast(packet: Packets.Packet, filter: ChannelMatcher = ChannelMatchers.all())
  fun broadcastToInViewChunk(packet: Packets.Packet, chunkX: ChunkCoord, chunkY: ChunkCoord, filter: ChannelMatcher = ChannelMatchers.all())
  fun broadcastToInView(packet: Packets.Packet, worldX: WorldCoord, worldY: WorldCoord, filter: ChannelMatcher = ChannelMatchers.all())
  fun broadcastToInView(packet: Packets.Packet, entity: Entity, filter: ChannelMatcher = ChannelMatchers.all())
}

object ClientPacketBroadcaster : PacketBroadcaster {

  private fun handleCall(packet: Packets.Packet) {
    error("Client tried to broadcast packet: ${singleLinePrinter.printToString(packet)}")
  }

  override fun broadcast(packet: Packets.Packet, filter: ChannelMatcher) = handleCall(packet)

  override fun broadcastToInViewChunk(packet: Packets.Packet, chunkX: ChunkCoord, chunkY: ChunkCoord, filter: ChannelMatcher) = handleCall(packet)

  override fun broadcastToInView(packet: Packets.Packet, worldX: WorldCoord, worldY: WorldCoord, filter: ChannelMatcher) = handleCall(packet)

  override fun broadcastToInView(packet: Packets.Packet, entity: Entity, filter: ChannelMatcher) = handleCall(packet)
}
