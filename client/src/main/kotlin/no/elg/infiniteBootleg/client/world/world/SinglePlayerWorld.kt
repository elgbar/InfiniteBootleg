package no.elg.infiniteBootleg.client.world.world

import no.elg.infiniteBootleg.core.events.InitialChunksOfWorldLoadedEvent
import no.elg.infiniteBootleg.core.events.api.EventManager.dispatchEvent
import no.elg.infiniteBootleg.core.util.launchOnAsync
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.TransientEntityTag.Companion.isTransientEntity
import no.elg.infiniteBootleg.core.world.ecs.load
import no.elg.infiniteBootleg.core.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.protobuf.ProtoWorld

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
