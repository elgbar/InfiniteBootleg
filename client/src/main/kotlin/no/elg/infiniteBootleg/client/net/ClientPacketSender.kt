package no.elg.infiniteBootleg.client.net

import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.core.net.PacketSender
import no.elg.infiniteBootleg.core.net.ServerClient
import no.elg.infiniteBootleg.core.net.ServerClient.Companion.sendServerBoundPacket
import no.elg.infiniteBootleg.core.util.ChunkCompactLoc
import no.elg.infiniteBootleg.protobuf.Packets

object ClientPacketSender : PacketSender {

  @Deprecated(
    "Use serverClient directly instead in the client module",
    ReplaceWith("ClientMain.inst().serverClient.sendServerBoundPacket(ifIsClient)", "no.elg.infiniteBootleg.client.main.ClientMain")
  )
  override fun sendDuplexPacket(ifIsServer: () -> Packets.Packet, ifIsClient: ServerClient.() -> Packets.Packet) {
    ClientMain.inst().serverClient.sendServerBoundPacket(ifIsClient)
  }

  @Deprecated(
    "Use serverClient directly instead in the client module",
    ReplaceWith("ClientMain.inst().serverClient.sendServerBoundPacket(ifIsClient)", "no.elg.infiniteBootleg.client.main.ClientMain")
  )
  override fun sendDuplexPacketInView(ifIsServer: () -> Pair<Packets.Packet?, ChunkCompactLoc>, ifIsClient: ServerClient.() -> Packets.Packet?) {
    ClientMain.inst().serverClient.sendServerBoundPacket(ifIsClient)
  }
}
