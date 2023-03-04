package no.elg.infiniteBootleg.server

import com.badlogic.ashley.core.Entity
import io.netty.channel.ChannelHandlerContext
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ServerClientWorld
import java.util.concurrent.CompletableFuture

/**
 * A client of a server
 *
 * @author Elg
 */
class ServerClient(
  val name: String,
  var world: ServerClientWorld? = null,
  var controllingEntity: ProtoWorld.Entity? = null
) {

  lateinit var ctx: ChannelHandlerContext
  var sharedInformation: SharedInformation? = null

  /**
   * If the client is fully initiated
   */
  var started: Boolean = false
  var chunksLoaded: Boolean = false

  val uuid get() = sharedInformation!!.entityUUID // FIXME
  var player: Entity? = null
  var futurePlayer: CompletableFuture<Entity>? = null
}
