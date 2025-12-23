package no.elg.infiniteBootleg.core.util

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.EventLoopGroup
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.nio.NioIoHandler

private val logger = KotlinLogging.logger {}

fun createEventLoopGroup(): EventLoopGroup {
  val ioHandlerFactory = try {
    EpollIoHandler.newFactory()
  } catch (_: Throwable) {
    logger.trace { "Failed to use EpollIoHandler (not on linux?), falling back to NioIoHandler" }
    NioIoHandler.newFactory()
  }
  return MultiThreadIoEventLoopGroup(ioHandlerFactory)
}
