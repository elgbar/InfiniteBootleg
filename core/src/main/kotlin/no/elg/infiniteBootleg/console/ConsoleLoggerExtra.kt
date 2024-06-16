package no.elg.infiniteBootleg.console

import com.google.protobuf.TextFormat
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Marker
import io.github.oshai.kotlinlogging.slf4j.toKotlinLogging
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.protobuf.Packets
import org.slf4j.MarkerFactory

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

val clientSideClientBoundMarker: Marker = MarkerFactory.getMarker("client<-server").toKotlinLogging()
val serverSideClientBoundMarker: Marker = MarkerFactory.getMarker("server->client").toKotlinLogging()

val clientSideServerBoundMarker: Marker = MarkerFactory.getMarker("client<-server").toKotlinLogging()
val serverSideServerBoundMarker: Marker = MarkerFactory.getMarker("server->client").toKotlinLogging()

fun logPacket(directionMarker: Marker, packet: Packets.Packet) {
  if (Settings.debug && Settings.logPackets && packet.type !in filterOutPackets) {
    logger.debug(null as Throwable?, directionMarker) { TextFormat.printer().shortDebugString(packet) }
  }
}
