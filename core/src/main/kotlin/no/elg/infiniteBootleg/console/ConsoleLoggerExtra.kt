package no.elg.infiniteBootleg.console

import com.google.protobuf.TextFormat
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.Packets

fun logPacket(direction: String, packet: Packets.Packet) {
  if (Settings.logPackets && packet.type !in Settings.filterOutPackets) {
    Main.logger().debug(direction) { TextFormat.printer().shortDebugString(packet) }
  }
}
