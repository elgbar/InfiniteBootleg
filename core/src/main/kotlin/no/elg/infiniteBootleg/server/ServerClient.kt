package no.elg.infiniteBootleg.server

import com.badlogic.ashley.core.Entity
import io.netty.channel.ChannelHandlerContext
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ClientWorld

/**
 * A client of a server
 *
 * @author Elg
 */
class ServerClient(
  val name: String,
  var world: ClientWorld? = null,
  var controllingEntity: ProtoWorld.Entity? = null
) {

  lateinit var ctx: ChannelHandlerContext
  var sharedInformation: SharedInformation? = null

  /**
   * If the client is fully initiated
   */
  var started: Boolean = false
  var chunksLoaded: Boolean = false

  private var backingPlayer: Entity? = null

  val uuid get() = sharedInformation!!.entityUUID // FIXME
  val player: Entity?
    get() {
      val bp = backingPlayer
      val clientWorld = world
      if (clientWorld != null && (bp == null)) {
        backingPlayer = clientWorld.getEntity(uuid)
      }
      return bp
    }
}
