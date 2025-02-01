package no.elg.infiniteBootleg.core.net

import no.elg.infiniteBootleg.core.util.ChunkCompactLoc
import no.elg.infiniteBootleg.protobuf.Packets.Packet

/**
 * Handle sending packets on clients and servers. This is the preferred method of sending packets on both server and client
 */
interface PacketSender {

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
