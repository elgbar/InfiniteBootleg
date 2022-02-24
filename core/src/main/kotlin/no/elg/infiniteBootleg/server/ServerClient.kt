package no.elg.infiniteBootleg.server

import io.netty.channel.ChannelHandlerContext
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity
import no.elg.infiniteBootleg.world.ClientWorld
import no.elg.infiniteBootleg.world.subgrid.enitites.Player

/**
 * A client of a server
 *
 * @author Elg
 */
class ServerClient(
  val name: String,
  var world: ClientWorld? = null,
  var controllingEntity: Entity? = null
) {

  lateinit var ctx: ChannelHandlerContext
  var sharedInformation: SharedInformation? = null

  /**
   * If the client is fully initiated
   */
  var started: Boolean = false
  var chunksLoaded: Boolean = false

  private var backingPlayer: Player? = null

  val uuid get() = sharedInformation!!.entityUUID // FIXME
  val player: Player?
    get() {
      // FIXME
      if (backingPlayer == null || backingPlayer!!.isInvalid) {
        backingPlayer = world!!.getPlayer(uuid)
      }
      return backingPlayer
    }
}
