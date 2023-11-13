package no.elg.infiniteBootleg.world.chunks

import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.events.BlockChangedEvent
import no.elg.infiniteBootleg.events.ChunkColumnUpdatedEvent
import no.elg.infiniteBootleg.events.api.EventListener
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.events.chunks.ChunkLightUpdatingEvent
import no.elg.infiniteBootleg.events.chunks.ChunkLoadedEvent
import no.elg.infiniteBootleg.util.isNeighbor
import no.elg.infiniteBootleg.util.isWithinRadius
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG

class ChunkListeners(private val chunk: ChunkImpl) : Disposable {

  private val updateChunkLightEventListener: EventListener<ChunkLightUpdatingEvent> = EventListener { (eventChunk, originLocalX, originLocalY): ChunkLightUpdatingEvent ->
    if (chunk.isNeighbor(eventChunk)) {
      chunk.doUpdateLight(eventChunk.getWorldX(originLocalX), eventChunk.getWorldY(originLocalY), checkDistance = true)
    }
  }

  private val updateLuminescentBlockChangedEventListener: EventListener<BlockChangedEvent> = EventListener { (oldBlock, newBlock): BlockChangedEvent ->
    val block = oldBlock ?: newBlock ?: return@EventListener
    if (block.chunk == chunk) return@EventListener
    if (chunk.isWithinRadius(block, 1f)) {
      chunk.queueForRendering(false)
    }
  }

  private val chunkColumnLightUpdatedListener: EventListener<ChunkColumnUpdatedEvent> = EventListener { event: ChunkColumnUpdatedEvent ->
    if (event.flag and BLOCKS_LIGHT_FLAG != 0) {
      chunk.doUpdateLightMultipleSources(event.calculatedDiffColumn(), checkDistance = true)
    }
  }

  private val chunkLoadedEventListener: EventListener<ChunkLoadedEvent> = EventListener { (eventChunk, _): ChunkLoadedEvent ->
    if (eventChunk.isNeighbor(chunk)) {
      chunk.doUpdateLight()
      chunk.queueForRendering(false)
    }
  }

  fun registerListeners() {
    EventManager.registerListener(updateChunkLightEventListener)
    EventManager.registerListener(updateLuminescentBlockChangedEventListener)
    EventManager.registerListener(chunkColumnLightUpdatedListener)
    EventManager.registerListener(chunkLoadedEventListener)
  }

  override fun dispose() {
    EventManager.removeListener(updateChunkLightEventListener)
    EventManager.removeListener(updateLuminescentBlockChangedEventListener)
    EventManager.removeListener(chunkColumnLightUpdatedListener)
    EventManager.removeListener(chunkLoadedEventListener)
  }
}
