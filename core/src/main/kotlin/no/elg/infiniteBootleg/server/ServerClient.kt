package no.elg.infiniteBootleg.server

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
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
) {

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
}
