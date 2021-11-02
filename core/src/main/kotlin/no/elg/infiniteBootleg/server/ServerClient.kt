package no.elg.infiniteBootleg.server

import io.netty.channel.ChannelHandlerContext
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.subgrid.enitites.Player

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

  /**
   * If the client is fully initiated
   */
  var started: Boolean = false

  private var backingPlayer: Player? = null

  val uuid get() = credentials!!.entityUUID //FIXME
  val player: Player?
    get() {
      //FIXME
      if (backingPlayer == null || backingPlayer!!.isInvalid) {
        backingPlayer = world!!.getPlayer(uuid)
      }
      return backingPlayer
    }
}
