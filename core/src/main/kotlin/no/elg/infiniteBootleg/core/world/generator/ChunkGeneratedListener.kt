package no.elg.infiniteBootleg.core.world.generator

import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.chunks.ChunkLoadedEvent
import no.elg.infiniteBootleg.core.world.generator.chunk.ChunkGenerator

class ChunkGeneratedListener(generator: ChunkGenerator) : Disposable {

  private val updateChunkLightEventListener =
    EventManager.registerListener { (chunk, isNewlyGenerated): ChunkLoadedEvent ->
      if (isNewlyGenerated && chunk.isValid) {
        generator.generateFeatures(chunk)
      }
    }

  override fun dispose() {
    updateChunkLightEventListener.removeListener()
  }
}
