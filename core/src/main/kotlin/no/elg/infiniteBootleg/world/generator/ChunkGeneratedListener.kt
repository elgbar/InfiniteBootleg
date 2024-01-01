package no.elg.infiniteBootleg.world.generator

import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.events.api.EventListener
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.events.chunks.ChunkLoadedEvent
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator

class ChunkGeneratedListener(generator: ChunkGenerator) : Disposable {

  private val updateChunkLightEventListener = EventListener { (chunk, isNewlyGenerated): ChunkLoadedEvent ->
    if (isNewlyGenerated) {
      Main.inst().scheduler.executeAsync {
        if (chunk.isValid) {
          generator.generateFeatures(chunk)
        }
      }
    }
  }

  init {
    EventManager.registerListener(updateChunkLightEventListener)
  }

  override fun dispose() {
    EventManager.removeListener(updateChunkLightEventListener)
  }
}
