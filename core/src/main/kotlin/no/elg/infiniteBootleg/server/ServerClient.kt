package no.elg.infiniteBootleg.server

import com.badlogic.gdx.utils.Disposable
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.netty.channel.ChannelFuture
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.Progress
import no.elg.infiniteBootleg.util.WorldCompactLoc
import no.elg.infiniteBootleg.world.world.ServerClientWorld
import java.time.Duration

/**
 * A client of a server
 *
 * @author Elg
 */
class ServerClient(
  val name: String,
  var world: ServerClientWorld? = null,
  var protoEntity: ProtoWorld.Entity? = null
) : Disposable {

  lateinit var ctx: ChannelHandlerContextWrapper
  var sharedInformation: SharedInformation? = null

  /**
   * If the client is fully initiated
   */
  var started: Boolean = false
  var chunksLoaded: Boolean = false

  val breakingBlockCache: Cache<WorldCompactLoc, Progress> by lazy {
    Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofMillis(250))
      .build()
  }

  val uuid get() = sharedInformation?.entityUUID ?: error("Cannot access uuid of entity before it is given by the server")

  /**
   * Sends a packet from the client to the server
   *
   * This method is to make the api of sending packets more uniform
   */
  @Suppress("NOTHING_TO_INLINE")
  inline fun sendServerBoundPacket(packet: Packets.Packet): ChannelFuture = ctx.writeAndFlushPacket(packet)

  override fun dispose() {
    sendServerBoundPacket { serverBoundClientDisconnectPacket("Server client dispoed") }
    ctx.disconnect()
  }

  companion object {
    /**
     * Sends a packet from the client to the server, given that [packet] and the [ServerClient] is not null
     */
    fun ServerClient?.sendServerBoundPacket(packet: ServerClient.() -> Packets.Packet?): ChannelFuture? = this?.run { packet()?.let { sendServerBoundPacket(it) } }

    /**
     * Sends multiple packets from the client to the server efficiently, given that [packet] and the [ServerClient] is not null
     */
    fun ServerClient?.sendServerBoundPackets(packets: ServerClient.() -> Iterable<Packets.Packet>?): List<ChannelFuture>? =
      this?.run {
        packets()?.map(ctx::writePacket).also { ctx.flush() }
      }
  }
}
