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
    // Do not log the exception stacktrace, as it will always fail on non-linux systems, its distracting
    logger.trace { "Failed to use EpollIoHandler (not on linux?), falling back to NioIoHandler" }
    NioIoHandler.newFactory()
  }
  val ofPlatform = Thread.ofPlatform().name(SERVER_THREAD_PREFIX, 0).daemon().factory()
  return MultiThreadIoEventLoopGroup(0, ofPlatform, ioHandlerFactory)
}
