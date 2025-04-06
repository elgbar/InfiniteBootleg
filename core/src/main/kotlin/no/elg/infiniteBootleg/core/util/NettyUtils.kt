package no.elg.infiniteBootleg.core.util

import io.netty.channel.EventLoopGroup
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.nio.NioIoHandler

fun createEventLoopGroup(): EventLoopGroup {
  val ioHandlerFactory = try {
    EpollIoHandler.newFactory()
  } catch (e: Throwable) {
    NioIoHandler.newFactory()
  }
  return MultiThreadIoEventLoopGroup(ioHandlerFactory)
}
