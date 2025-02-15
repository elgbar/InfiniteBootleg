package no.elg.infiniteBootleg.core.console

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Marker
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.util.singleLinePrinter
import no.elg.infiniteBootleg.protobuf.Packets

private val logger = KotlinLogging.logger {}

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

fun logPacket(directionMarker: Marker, packet: Packets.Packet) {
  if (Settings.logPackets && packet.type !in filterOutPackets) {
    logger.debug(null as Throwable?, directionMarker) { singleLinePrinter.printToString(packet) }
  }
}
