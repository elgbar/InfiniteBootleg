package no.elg.infiniteBootleg.client.world.world

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.events.InitialChunksOfWorldLoadedEvent
import no.elg.infiniteBootleg.core.events.api.EventManager.dispatchEvent
import no.elg.infiniteBootleg.core.util.launchOnAsync
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.TransientEntityTag.Companion.isTransientEntity
import no.elg.infiniteBootleg.core.world.ecs.creation.createNewPlayer
import no.elg.infiniteBootleg.core.world.ecs.load
import no.elg.infiniteBootleg.core.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.protobuf.ProtoWorld

private val logger = KotlinLogging.logger {}

class SinglePlayerWorld(generator: ChunkGenerator, seed: Long, worldName: String, forceTransient: Boolean = false) : ClientWorld(generator, seed, worldName, forceTransient) {

  /**
   * Load singleplayer player from the given [protoWorld] or create a new player if none is found
   */
  private fun loadPlayer(protoWorld: ProtoWorld.World?): Boolean {
    val futurePlayer = if (protoWorld != null && protoWorld.hasPlayer()) {
      logger.debug { "Spawning existing singleplayer player" }
      val playerPosition = protoWorld.player.position
      render.lookAt(playerPosition.x, playerPosition.y)
      this@SinglePlayerWorld.load(protoWorld.player)
    } else {
      logger.debug { "Spawning new singleplayer player" }
      render.lookAt(spawn)
      this@SinglePlayerWorld.createNewPlayer()
    }

    launchOnAsync {
      // blocking, will prevent InitialChunksOfWorldLoadedEvent from being dispatched until all chunks are loaded
      render.chunkLocationsInView.forEach(::loadChunk)
      logger.debug { "Loaded initial chunks" }

      futurePlayer.thenApply { player ->
        logger.debug { "Singleplayer player created" }
        player.isTransientEntity = true
        dispatchEvent(InitialChunksOfWorldLoadedEvent(this@SinglePlayerWorld))
      }
    }
    return true
  }

  override fun loadNewWorld(): Boolean {
    super.loadNewWorld()
    return loadPlayer(null)
  }

  override fun loadFromProtoWorld(protoWorld: ProtoWorld.World): Boolean {
    super.loadFromProtoWorld(protoWorld)
    return loadPlayer(protoWorld)
  }
}
