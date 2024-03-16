package no.elg.infiniteBootleg.server

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
import no.elg.infiniteBootleg.console.logPacket
import no.elg.infiniteBootleg.protobuf.Packets.Packet
import no.elg.infiniteBootleg.util.IllegalAction
import java.net.SocketAddress

@Suppress("NOTHING_TO_INLINE", "DEPRECATION")
class ChannelHandlerContextWrapper(val direction: String, private val handler: ChannelHandlerContext) : ChannelHandlerContext {

  private fun logPacketWrite(msg: Any) {
    if (msg is Packet) {
      logPacket(direction, msg)
    } else {
      IllegalAction.STACKTRACE.handle("NETTY") {
        "Tried to send a non packet (type: ${msg::class}) toString: $msg"
      }
    }
  }

  @Deprecated("Use Channel#attr(AttributeKey) instead.")
  override fun <T : Any> attr(key: AttributeKey<T>): Attribute<T> = handler.attr(key)

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

  inline fun writePacket(msg: Packet): ChannelFuture = write(msg)

  @Deprecated("Unsafe type", replaceWith = ReplaceWith("this.writePacket(msg)"))
  override fun write(msg: Any): ChannelFuture {
    logPacketWrite(msg)
    return handler.write(msg)
  }

  inline fun writePacket(msg: Packet, promise: ChannelPromise): ChannelFuture = write(msg, promise)

  @Deprecated("Unsafe type", replaceWith = ReplaceWith("this.writePacket(msg, promise)"))
  override fun write(msg: Any, promise: ChannelPromise): ChannelFuture {
    logPacketWrite(msg)
    return handler.write(msg, promise)
  }

  override fun flush(): ChannelHandlerContext = handler.flush()

  inline fun writeAndFlushPacket(msg: Packet, promise: ChannelPromise): ChannelFuture = writeAndFlush(msg, promise)

  @Deprecated("Unsafe type", replaceWith = ReplaceWith("this.writeAndFlushPacket(msg, promise)"))
  override fun writeAndFlush(msg: Any, promise: ChannelPromise): ChannelFuture {
    logPacketWrite(msg)
    return handler.writeAndFlush(msg, promise)
  }

  inline fun writeAndFlushPacket(msg: Packet): ChannelFuture = writeAndFlush(msg)

  @Deprecated("Unsafe type", replaceWith = ReplaceWith("this.writeAndFlushPacket(msg)"))
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
