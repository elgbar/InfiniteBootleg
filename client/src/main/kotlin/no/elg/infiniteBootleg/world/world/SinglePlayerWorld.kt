package no.elg.infiniteBootleg.world.world

import no.elg.infiniteBootleg.events.InitialChunksOfWorldLoadedEvent
import no.elg.infiniteBootleg.events.api.EventManager.dispatchEvent
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.launchOnAsync
import no.elg.infiniteBootleg.world.ecs.components.transients.tags.TransientEntityTag.Companion.isTransientEntity
import no.elg.infiniteBootleg.world.ecs.load
import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator

class SinglePlayerWorld(generator: ChunkGenerator, seed: Long, worldName: String, forceTransient: Boolean = false) : ClientWorld(generator, seed, worldName, forceTransient) {
  override fun initialize() {
    super.initialize()
    render.lookAt(spawn)
  }

  override fun loadFromProtoWorld(protoWorld: ProtoWorld.World): Boolean {
    super.loadFromProtoWorld(protoWorld)
    return if (protoWorld.hasPlayer()) {
      launchOnAsync {
        val playerPosition = protoWorld.player.position
        render.lookAt(playerPosition.x, playerPosition.y)
        render.chunkLocationsInView.forEach(::loadChunk)
        this@SinglePlayerWorld.load(protoWorld.player).thenApply {
          it.isTransientEntity = true
          dispatchEvent(InitialChunksOfWorldLoadedEvent(this@SinglePlayerWorld))
        }
      }
      true
    } else {
      false
    }
  }
}
