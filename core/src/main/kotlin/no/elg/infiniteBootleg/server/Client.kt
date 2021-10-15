package no.elg.infiniteBootleg.server

import io.netty.channel.ChannelHandlerContext
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity
import no.elg.infiniteBootleg.world.World

/**
 * @author Elg
 */
class Client(
  val name: String,
  var world: World? = null,
  var controllingEntity: Entity? = null
) {
  
  lateinit var ctx: ChannelHandlerContext
}
