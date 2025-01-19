package no.elg.infiniteBootleg.server.server

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.server.ChannelHandlerContextWrapper
import no.elg.infiniteBootleg.server.clientBoundDisconnectPlayerPacket

private val logger = KotlinLogging.logger {}

fun ChannelHandlerContextWrapper.fatal(msg: String) {
  this.writeAndFlushPacket(clientBoundDisconnectPlayerPacket(msg))
  logger.error { msg }
}
