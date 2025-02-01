package no.elg.infiniteBootleg.net

import no.elg.infiniteBootleg.protobuf.Packets.Packet
import no.elg.infiniteBootleg.util.ChunkCompactLoc

/**
 * Handle sending packets on clients and servers. This is the preferred method of sending packets on both server and client
 */
interface PacketSender {
//  fun broadcast(packet: Packet, filter: ChannelMatcher = ChannelMatchers.all())
//

//  fun broadcastToInViewChunk(packet: Packet, chunkX: ChunkCoord, chunkY: ChunkCoord, filter: ChannelMatcher = ChannelMatchers.all())
//  fun broadcastToInView(packet: Packet, worldX: WorldCoord, worldY: WorldCoord, filter: ChannelMatcher = ChannelMatchers.all())
//  fun broadcastToInView(packet: Packet, entity: Entity, filter: ChannelMatcher = ChannelMatchers.all())

  /**
   * Helper method to send either a server bound or client bound packet depending on which this instance currently is.
   *
   * @param ifIsServer The packet to send if we are the server, and the chunk to send it to
   * @param ifIsClient The packet to send if we are a server client
   */
  fun sendDuplexPacketInView(ifIsServer: () -> Pair<Packet?, ChunkCompactLoc>, ifIsClient: ServerClient.() -> Packet?)

  /**
   * Helper method to send either a server bound or client bound packet depending on which this instance currently is.
   *
   * @param ifIsServer The packet to send if we are the server
   * @param ifIsClient The packet to send if we are a server client
   */
  fun sendDuplexPacket(ifIsServer: () -> Packet, ifIsClient: ServerClient.() -> Packet)
}
