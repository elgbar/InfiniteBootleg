package no.elg.infiniteBootleg.server.net

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.net.ChannelHandlerContextWrapper
import no.elg.infiniteBootleg.core.net.clientBoundDisconnectPlayerPacket

private val logger = KotlinLogging.logger {}

fun ChannelHandlerContextWrapper.fatal(msg: String) {
  this.writeAndFlushPacket(clientBoundDisconnectPlayerPacket(msg))
  logger.error { msg }
}
