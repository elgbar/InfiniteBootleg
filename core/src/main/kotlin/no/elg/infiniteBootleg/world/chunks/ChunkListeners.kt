package no.elg.infiniteBootleg.world.chunks

import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.events.BlockChangedEvent
import no.elg.infiniteBootleg.events.ChunkColumnUpdatedEvent
import no.elg.infiniteBootleg.events.WorldTickedEvent
import no.elg.infiniteBootleg.events.api.EventListener
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.events.chunks.ChunkLightChangedEvent
import no.elg.infiniteBootleg.events.chunks.ChunkLoadedEvent
import no.elg.infiniteBootleg.util.WorldCompactLoc
import no.elg.infiniteBootleg.util.WorldCompactLocArray
import no.elg.infiniteBootleg.util.compactChunkToWorld
import no.elg.infiniteBootleg.util.isNeighbor
import no.elg.infiniteBootleg.util.isWithinRadius
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG

class ChunkListeners(private val chunk: ChunkImpl) : Disposable {

  private val lightLocs: MutableList<WorldCompactLoc> = mutableListOf()

  /**
   * Actually update the light of the chunk based on lights that have been queued by [registerLightChangeForNearbyChunks]
   */
  private val updateChunkLightsOnWorldTick: EventListener<WorldTickedEvent> = EventListener {
    if (it.world == chunk.world) {
      val lights: WorldCompactLocArray = synchronized(lightLocs) {
        if (lightLocs.isEmpty()) {
          // No need to update lights, do a fast return
          return@EventListener
        }
        lightLocs.toLongArray().also {
          lightLocs.clear()
        }
      }
      chunk.doUpdateLightMultipleSources(lights, checkDistance = true)
    }
  }

  /**
   * Register a location to be updated when the world ticks
   */
  private val registerLightChangeForNearbyChunks: EventListener<ChunkLightChangedEvent> = EventListener { (eventChunk, originLocalX, originLocalY): ChunkLightChangedEvent ->
    if (chunk.isNeighbor(eventChunk) || chunk == eventChunk) {
      val compactLoc = compactChunkToWorld(eventChunk, originLocalX, originLocalY)
      synchronized(lightLocs) {
        lightLocs += compactLoc
      }
    }
  }

  /**
   * Update the texture of this chunk if a blocks changes either in this chunk or in a neighbor chunk
   */
  private val updateChunkTextureOnBlockChange: EventListener<BlockChangedEvent> = EventListener { (oldBlock, newBlock): BlockChangedEvent ->
    val block = oldBlock ?: newBlock ?: return@EventListener
    if (block.chunk == chunk) return@EventListener
    if (chunk.isWithinRadius(block, 1f)) {
      chunk.queueForRendering(false)
    }
  }

  /**
   * Update chunk light when a chunk column is updated
   */
  private val updateChunkLightOnChunkColumnUpdatedEvent: EventListener<ChunkColumnUpdatedEvent> = EventListener { event: ChunkColumnUpdatedEvent ->
    if (event.flag and BLOCKS_LIGHT_FLAG != 0) {
      chunk.doUpdateLightMultipleSources(event.calculatedDiffColumn, checkDistance = true)
    }
  }

  /**
   * When a neighbor chunk is loaded we might have to update the lights or the textures of this chunk since it might contain lights that
   * affect this chunk or the blocks that change the texture of this chunk
   */
  private val updateChunkTextureAndLightOnNeighborChunkLoaded: EventListener<ChunkLoadedEvent> = EventListener { (eventChunk, _): ChunkLoadedEvent ->
    if (eventChunk.isNeighbor(chunk)) {
      chunk.updateAllBlockLights()
      chunk.queueForRendering(false)
    }
  }

  fun registerListeners() {
    EventManager.registerListener(listener = registerLightChangeForNearbyChunks)
    EventManager.registerListener(listener = updateChunkTextureOnBlockChange)
    EventManager.registerListener(listener = updateChunkLightOnChunkColumnUpdatedEvent)
    EventManager.registerListener(listener = updateChunkTextureAndLightOnNeighborChunkLoaded)
    EventManager.registerListener(listener = updateChunkLightsOnWorldTick)
  }

  override fun dispose() {
    EventManager.removeListener(registerLightChangeForNearbyChunks)
    EventManager.removeListener(updateChunkTextureOnBlockChange)
    EventManager.removeListener(updateChunkLightOnChunkColumnUpdatedEvent)
    EventManager.removeListener(updateChunkTextureAndLightOnNeighborChunkLoaded)
    EventManager.removeListener(updateChunkLightsOnWorldTick)
  }
}
