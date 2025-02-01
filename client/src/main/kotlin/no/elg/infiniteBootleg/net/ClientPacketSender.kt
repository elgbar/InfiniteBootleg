package no.elg.infiniteBootleg.net

import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.util.ChunkCompactLoc

object ClientPacketSender : PacketSender {
//
//  private fun handleCall(packet: Packets.Packet) {
//    error("Client tried to broadcast packet: ${singleLinePrinter.printToString(packet)}")
//  }
//
//  @Deprecated("Cannot broadcast as a client", level = DeprecationLevel.ERROR)
//  override fun broadcast(packet: Packets.Packet, filter: ChannelMatcher) = handleCall(packet)
//
//  @Deprecated("Cannot broadcast as a client", level = DeprecationLevel.ERROR)
//  override fun broadcastToInViewChunk(packet: Packets.Packet, chunkX: ChunkCoord, chunkY: ChunkCoord, filter: ChannelMatcher) = handleCall(packet)
//
//  @Deprecated("Cannot broadcast as a client", level = DeprecationLevel.ERROR)
//  override fun broadcastToInView(packet: Packets.Packet, worldX: WorldCoord, worldY: WorldCoord, filter: ChannelMatcher) = handleCall(packet)
//
//  @Deprecated("Cannot broadcast as a client", level = DeprecationLevel.ERROR)
//  override fun broadcastToInView(packet: Packets.Packet, entity: Entity, filter: ChannelMatcher) = handleCall(packet)

  override fun sendDuplexPacket(ifIsServer: () -> Packets.Packet, ifIsClient: ServerClient.() -> Packets.Packet) {
    if (Main.Companion.isServerClient) {
      val client = ClientMain.inst().serverClient ?: error("Server client null after check")
      client.ctx.writeAndFlushPacket(client.ifIsClient())
    }
  }

  override fun sendDuplexPacketInView(ifIsServer: () -> Pair<Packets.Packet?, ChunkCompactLoc>, ifIsClient: ServerClient.() -> Packets.Packet?) {
    if (Main.Companion.isServerClient) {
      val client = ClientMain.inst().serverClient ?: error("Server client null after check")
      val packet = client.ifIsClient() ?: return
      client.ctx.writeAndFlushPacket(packet)
    }
  }
}
