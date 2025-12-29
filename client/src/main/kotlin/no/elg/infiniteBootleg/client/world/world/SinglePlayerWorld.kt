package no.elg.infiniteBootleg.client.world.world

import com.badlogic.ashley.core.Entity
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.events.InitialChunksOfWorldLoadedEvent
import no.elg.infiniteBootleg.core.events.WorldLoadedEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.api.EventManager.dispatchEvent
import no.elg.infiniteBootleg.core.util.launchOnAsyncSuspendable
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.core.world.ecs.components.tags.FlyingTag.Companion.ensureFlyingStatus
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.TransientEntityTag.Companion.isTransientEntity
import no.elg.infiniteBootleg.core.world.ecs.creation.createNewPlayer
import no.elg.infiniteBootleg.core.world.ecs.load
import no.elg.infiniteBootleg.core.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.core.world.loader.chunk.ChunkLoader
import no.elg.infiniteBootleg.core.world.loader.chunk.FullChunkLoader
import no.elg.infiniteBootleg.protobuf.ProtoWorld

private val logger = KotlinLogging.logger {}

class SinglePlayerWorld(generator: ChunkGenerator, seed: Long, worldName: String, forceTransient: Boolean = false) : ClientWorld(generator, seed, worldName, forceTransient) {
  override val chunkLoader: ChunkLoader = FullChunkLoader(this, generator)

  /**
   * Load singleplayer player from the given [protoWorld] or create a new player if none is found
   */
  private fun loadWorldAndPlayer(protoWorld: ProtoWorld.World?): Boolean {
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

    var worldLoaded = false
    futurePlayer.thenApply { player ->
      if (!worldLoaded) {
        player.box2d.disableGravity()
        player.setVelocity(0f, 0f)
      }
      player.isTransientEntity = false
      logger.debug { "Singleplayer player created" }
    }

    launchOnAsyncSuspendable {
      // blocking, will prevent InitialChunksOfWorldLoadedEvent from being dispatched until all chunks are loaded
      render.chunkLocationsInView.forEach(::loadChunk)
      logger.debug { "Loaded initial chunks" }
      dispatchEvent(InitialChunksOfWorldLoadedEvent(this@SinglePlayerWorld))
    }
    EventManager.oneShotListener<WorldLoadedEvent> {
      futurePlayer.thenApply { player ->
        worldLoaded = true
        player.ensureFlyingStatus()
      }
    }
    return true
  }

  override fun loadNewWorld(): Boolean {
    super.loadNewWorld()
    return loadWorldAndPlayer(null)
  }

  override fun loadFromProtoWorld(protoWorld: ProtoWorld.World): Boolean {
    super.loadFromProtoWorld(protoWorld)
    return loadWorldAndPlayer(protoWorld)
  }

  override fun isAuthorizedToChange(entity: Entity): Boolean = true
}
