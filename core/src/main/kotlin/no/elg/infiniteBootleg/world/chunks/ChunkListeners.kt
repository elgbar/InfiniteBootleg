package no.elg.infiniteBootleg.world.chunks

import com.badlogic.gdx.utils.Disposable
import ktx.collections.GdxLongArray
import no.elg.infiniteBootleg.events.BlockChangedEvent
import no.elg.infiniteBootleg.events.ChunkColumnUpdatedEvent
import no.elg.infiniteBootleg.events.WorldTickedEvent
import no.elg.infiniteBootleg.events.api.EventManager.registerListener
import no.elg.infiniteBootleg.events.api.RegisteredEventListener
import no.elg.infiniteBootleg.events.chunks.ChunkLightChangedEvent
import no.elg.infiniteBootleg.events.chunks.ChunkLoadedEvent
import no.elg.infiniteBootleg.util.WorldCompactLocArray
import no.elg.infiniteBootleg.util.compactChunkToWorld
import no.elg.infiniteBootleg.util.isNeighbor
import no.elg.infiniteBootleg.util.isWithinRadius
import no.elg.infiniteBootleg.world.blocks.Block.Companion.queryEntities
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG

class ChunkListeners(private val chunk: ChunkImpl) : Disposable {

  private val listeners: MutableSet<RegisteredEventListener> = mutableSetOf()

  private val lightLocs: GdxLongArray = GdxLongArray(false, 16)

  fun registerListeners() {
    require(listeners.isEmpty()) { "Listeners cannot be registered twice" }

    /**
     * Actually update the light of the chunk based on lights that have been queued by [registerLightChangeForNearbyChunks]
     */
    listeners += registerListener<WorldTickedEvent> {
      if (it.world == chunk.world) {
        val lights: WorldCompactLocArray = synchronized(lightLocs) {
          if (lightLocs.isEmpty()) {
            // No need to update lights, do a fast return
            return@registerListener
          }
          lightLocs.items.copyOf().also {
            lightLocs.clear()
            lightLocs.shrink()
          }
        }
        chunk.doUpdateLightMultipleSources(lights, checkDistance = true)
      }
    }

    /**
     * Register a location to be updated when the world ticks
     */
    listeners += registerListener { (eventChunk, originLocalX, originLocalY): ChunkLightChangedEvent ->
      if (chunk.isNeighbor(eventChunk) || chunk == eventChunk) {
        val compactLoc = compactChunkToWorld(eventChunk, originLocalX, originLocalY)
        synchronized(lightLocs) {
          lightLocs.add(compactLoc)
        }
      }
    }

    /**
     * Update the texture of this chunk if a blocks changes either in this chunk or in a neighbor chunk
     */
    listeners += registerListener { (oldBlock, newBlock): BlockChangedEvent ->
      val block = oldBlock ?: newBlock ?: return@registerListener
      if (block.chunk == chunk) return@registerListener
      if (chunk.isWithinRadius(block, 1f)) {
        chunk.queueForRendering(false)
      }
    }

    val chunkLookRange = (chunk.chunkX - 2)..(chunk.chunkX + 2)
    /**
     * Update chunk light when a chunk column is updated
     */
    listeners += registerListener { event: ChunkColumnUpdatedEvent ->
      if (event.chunkX in chunkLookRange && (event.flag and BLOCKS_LIGHT_FLAG != 0)) {
        val lights: WorldCompactLocArray = event.calculatedDiffColumn
        synchronized(lightLocs) {
          lightLocs.addAll(lights, 0, lights.size)
        }
      }
    }

    /**
     * When a neighbor chunk is loaded we might have to update the lights or the textures of this chunk since it might contain lights that
     * affect this chunk or the blocks that change the texture of this chunk
     */
    listeners += registerListener { (eventChunk, _): ChunkLoadedEvent ->
      if (eventChunk.isNeighbor(chunk)) {
        chunk.updateAllBlockLights()
      }
    }

    /**
     * Awakens players to allow them to jump in a hole when placing a block
     */
    listeners += registerListener { (oldBlock, newBlock): BlockChangedEvent ->
      val block = oldBlock ?: newBlock
      if (block?.chunk === chunk) {
        block.queryEntities {
          for ((body, _) in it) {
            body.isAwake = true
          }
        }
      }
    }
  }

  override fun dispose() {
    listeners.forEach(RegisteredEventListener::removeListener)
  }
}
