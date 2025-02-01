package no.elg.infiniteBootleg.client.net

import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.net.PacketSender
import no.elg.infiniteBootleg.core.net.ServerClient
import no.elg.infiniteBootleg.core.util.ChunkCompactLoc
import no.elg.infiniteBootleg.protobuf.Packets

object ClientPacketSender : PacketSender {

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
