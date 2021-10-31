package no.elg.infiniteBootleg.server

import io.netty.channel.ChannelHandlerContext
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity
import no.elg.infiniteBootleg.world.World

/**
 * @author Elg
 */
class ServerClient(
  val name: String,
  var world: World? = null,
  var controllingEntity: Entity? = null
) {

  lateinit var ctx: ChannelHandlerContext
  var credentials: ConnectionCredentials? = null

  val uuid get() = credentials!!.entityUUID //FIXME
}
