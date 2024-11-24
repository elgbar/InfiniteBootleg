package no.elg.infiniteBootleg.world.generator

import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.events.api.EventManager.registerListener
import no.elg.infiniteBootleg.events.chunks.ChunkLoadedEvent
import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator

class ChunkGeneratedListener(generator: ChunkGenerator) : Disposable {

  private val updateChunkLightEventListener = registerListener { (chunk, isNewlyGenerated): ChunkLoadedEvent ->
    if (isNewlyGenerated && chunk.isValid) {
      generator.generateFeatures(chunk)
    }
  }

  override fun dispose() {
    updateChunkLightEventListener.removeListener()
  }
}
