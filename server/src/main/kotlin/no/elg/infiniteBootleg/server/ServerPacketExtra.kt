package no.elg.infiniteBootleg.server

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun ChannelHandlerContextWrapper.fatal(msg: String) {
  this.writeAndFlushPacket(clientBoundDisconnectPlayerPacket(msg))
  logger.error { msg }
}
