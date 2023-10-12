package no.elg.infiniteBootleg.server

import com.google.protobuf.TextFormat
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPipeline
import io.netty.channel.ChannelProgressivePromise
import io.netty.channel.ChannelPromise
import io.netty.util.Attribute
import io.netty.util.AttributeKey
import io.netty.util.concurrent.EventExecutor
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.Packets.Packet
import java.net.SocketAddress

class ChannelHandlerContextWrapper(val direction: String, private val handler: ChannelHandlerContext) : ChannelHandlerContext {

  private fun logPacketWrite(msg: Any) {
    if (msg is Packet) {
      Main.logger().debug(direction) { TextFormat.printer().shortDebugString(msg) }
    } else {
      Main.logger().error("Tried to send a non packet (type: ${msg::class}) toString: $msg")
    }
  }

  @Suppress("DEPRECATION")
  @Deprecated("Use Channel#attr(AttributeKey) instead.")
  override fun <T : Any> attr(key: AttributeKey<T>): Attribute<T> = handler.attr(key)

  @Suppress("DEPRECATION")
  @Deprecated("Use Channel#hasAttr(AttributeKey) instead.")
  override fun <T : Any> hasAttr(key: AttributeKey<T>): Boolean = handler.hasAttr(key)

  override fun fireChannelRegistered(): ChannelHandlerContext = handler.fireChannelRegistered()

  override fun fireChannelUnregistered(): ChannelHandlerContext = handler.fireChannelUnregistered()

  override fun fireChannelActive(): ChannelHandlerContext = handler.fireChannelActive()

  override fun fireChannelInactive(): ChannelHandlerContext = handler.fireChannelInactive()

  override fun fireExceptionCaught(cause: Throwable): ChannelHandlerContext = handler.fireExceptionCaught(cause)

  override fun fireUserEventTriggered(evt: Any): ChannelHandlerContext = handler.fireUserEventTriggered(evt)

  override fun fireChannelRead(msg: Any): ChannelHandlerContext = handler.fireChannelRead(msg)

  override fun fireChannelReadComplete(): ChannelHandlerContext = handler.fireChannelReadComplete()

  override fun fireChannelWritabilityChanged(): ChannelHandlerContext = handler.fireChannelWritabilityChanged()

  override fun bind(localAddress: SocketAddress): ChannelFuture = handler.bind(localAddress)

  override fun bind(localAddress: SocketAddress, promise: ChannelPromise): ChannelFuture = handler.bind(localAddress, promise)

  override fun connect(remoteAddress: SocketAddress): ChannelFuture = handler.connect(remoteAddress)

  override fun connect(remoteAddress: SocketAddress, localAddress: SocketAddress): ChannelFuture = handler.connect(remoteAddress, localAddress)

  override fun connect(remoteAddress: SocketAddress, promise: ChannelPromise): ChannelFuture = handler.connect(remoteAddress, promise)

  override fun connect(remoteAddress: SocketAddress, localAddress: SocketAddress, promise: ChannelPromise): ChannelFuture = handler.connect(remoteAddress, localAddress, promise)

  override fun disconnect(): ChannelFuture = handler.disconnect()

  override fun disconnect(promise: ChannelPromise): ChannelFuture = handler.disconnect(promise)

  override fun close(): ChannelFuture = handler.close()

  override fun close(promise: ChannelPromise): ChannelFuture = handler.close(promise)

  override fun deregister(): ChannelFuture = handler.deregister()

  override fun deregister(promise: ChannelPromise): ChannelFuture = handler.deregister(promise)

  override fun read(): ChannelHandlerContext = handler.read()

  override fun write(msg: Any): ChannelFuture {
    logPacketWrite(msg)
    return handler.write(msg)
  }

  override fun write(msg: Any, promise: ChannelPromise): ChannelFuture {
    logPacketWrite(msg)
    return handler.write(msg, promise)
  }

  override fun flush(): ChannelHandlerContext = handler.flush()

  override fun writeAndFlush(msg: Any, promise: ChannelPromise): ChannelFuture {
    logPacketWrite(msg)
    return handler.writeAndFlush(msg, promise)
  }

  override fun writeAndFlush(msg: Any): ChannelFuture {
    logPacketWrite(msg)
    return handler.writeAndFlush(msg)
  }

  override fun newPromise(): ChannelPromise = handler.newPromise()

  override fun newProgressivePromise(): ChannelProgressivePromise = handler.newProgressivePromise()

  override fun newSucceededFuture(): ChannelFuture = handler.newSucceededFuture()

  override fun newFailedFuture(cause: Throwable): ChannelFuture = handler.newFailedFuture(cause)

  override fun voidPromise(): ChannelPromise = handler.voidPromise()

  override fun channel(): Channel = handler.channel()

  override fun executor(): EventExecutor = handler.executor()

  override fun name(): String = handler.name()

  override fun handler(): ChannelHandler = handler.handler()

  override fun isRemoved(): Boolean = handler.isRemoved

  override fun pipeline(): ChannelPipeline = handler.pipeline()

  override fun alloc(): ByteBufAllocator = handler.alloc()
}
