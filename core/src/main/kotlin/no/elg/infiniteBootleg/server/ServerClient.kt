package no.elg.infiniteBootleg.server

import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.world.ServerClientWorld

/**
 * A client of a server
 *
 * @author Elg
 */
class ServerClient(
  val name: String,
  var world: ServerClientWorld? = null,
  var protoEntity: ProtoWorld.Entity? = null
) {

  lateinit var ctx: ChannelHandlerContextWrapper
  var sharedInformation: SharedInformation? = null

  /**
   * If the client is fully initiated
   */
  var started: Boolean = false
  var chunksLoaded: Boolean = false

  val uuid get() = sharedInformation?.entityUUID ?: error("Cannot access uuid of entity before it is given by the server")
}
