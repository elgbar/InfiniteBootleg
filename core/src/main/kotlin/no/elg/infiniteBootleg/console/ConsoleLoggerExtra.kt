package no.elg.infiniteBootleg.console

import com.google.protobuf.TextFormat
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.protobuf.Packets

/**
 * Never log these packets
 */
val filterOutPackets = mutableSetOf<Packets.Packet.Type>().also {
  it += Packets.Packet.Type.DX_HEARTBEAT
  it += Packets.Packet.Type.DX_MOVE_ENTITY
}

fun temporallyFilterPacket(vararg packets: Packets.Packet.Type, block: () -> Unit) {
  val packetsSet = packets.toSet()
  filterOutPackets.addAll(packetsSet)
  try {
    block()
  } finally {
    filterOutPackets.removeAll(packetsSet)
  }
}

fun logPacket(direction: String, packet: Packets.Packet) {
  if (Settings.debug && Settings.logPackets && packet.type !in filterOutPackets) {
    KotlinLogging.logger(direction).debug { TextFormat.printer().shortDebugString(packet) }
  }
}
