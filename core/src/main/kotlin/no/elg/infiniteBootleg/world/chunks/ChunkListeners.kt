package no.elg.infiniteBootleg.world.chunks

import com.badlogic.gdx.utils.Disposable
import io.github.oshai.kotlinlogging.KotlinLogging
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.events.BlockChangedEvent
import no.elg.infiniteBootleg.events.ChunkColumnUpdatedEvent
import no.elg.infiniteBootleg.events.WorldTickedEvent
import no.elg.infiniteBootleg.events.api.Event
import no.elg.infiniteBootleg.events.api.EventListener
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.events.api.RegisteredEventListener
import no.elg.infiniteBootleg.events.chunks.ChunkLightChangedEvent
import no.elg.infiniteBootleg.events.chunks.ChunkLoadedEvent
import no.elg.infiniteBootleg.util.IllegalAction
import no.elg.infiniteBootleg.util.WorldCompactLocArray
import no.elg.infiniteBootleg.util.compactChunkToWorld
import no.elg.infiniteBootleg.util.isNeighbor
import no.elg.infiniteBootleg.util.isWithinRadius
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.world.blocks.Block.Companion.queryEntities
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG

private val logger = KotlinLogging.logger {}

class ChunkListeners(private val chunk: ChunkImpl) : Disposable {

  private var listeners: Array<RegisteredEventListener>? = null
  private val lightLocs = LongOpenHashSet()

  val chunkLookRange = (chunk.chunkX - 2)..(chunk.chunkX + 2)
  val chunkCompactLocation = chunk.compactLocation

  fun registerListeners() {
    require(listeners == null) { "Listeners cannot be registered twice" }

    listeners = arrayOf(
      registerListenerConditionally { (oldBlock, newBlock): BlockChangedEvent ->
        // Note: there are two events registered in the same listener
        val block = oldBlock ?: newBlock ?: return@registerListenerConditionally
        if (block.chunk === chunk) {
          // Awakens players to allow them to jump in a hole when placing a block
          block.queryEntities {
            for ((body, _) in it) {
              body.isAwake = true
            }
          }
        } else {
          // Update the texture of this chunk if a blocks changes either in this chunk or in a neighbor chunk
          if (chunk.isWithinRadius(block, 1f)) {
            chunk.queueForRendering(false)
          }
        }
      },
      /*
       * Actually update the light of the chunk based on lights that have been queued by [registerLightChangeForNearbyChunks]
       */
      registerListenerConditionally { event: WorldTickedEvent ->
        if (event.world == chunk.world) {
          val sources = lightLocs
          synchronized(sources) {
            if (sources.isEmpty()) return@registerListenerConditionally
            chunk.doUpdateLightMultipleSources(sources.toLongArray(), checkDistance = true)
            sources.clear()
          }
        } else {
          logger.warn { "Failed to update lights for ${stringifyCompactLoc(chunk)}" }
        }
      },

      /*
       * Register a location to be updated when the world ticks
       */
      registerListenerConditionally { (chunkLoc, originLocalX, originLocalY): ChunkLightChangedEvent ->
        if (chunkCompactLocation == chunkLoc || chunk.isNeighbor(chunkLoc)) {
          val compactLoc = compactChunkToWorld(chunkLoc, originLocalX, originLocalY)
          val sources = lightLocs
          synchronized(sources) {
            sources.add(compactLoc)
          }
        }
      },

      /*
       * Update chunk light when a chunk column is updated
       */
      registerListenerConditionally { event: ChunkColumnUpdatedEvent ->
        if ((event.flag and BLOCKS_LIGHT_FLAG != 0) && event.chunkX in chunkLookRange) {
          val lights: WorldCompactLocArray = event.calculatedDiffColumn
          val sources = lightLocs
          synchronized(sources) {
            sources.ensureCapacity(sources.size + lights.size)
            for (pos in lights) {
              sources.add(pos)
            }
          }
        }
      },

      /*
       * When a neighbor chunk is loaded we might have to update the lights or the textures of this chunk since it might contain lights that
       * affect this chunk or the blocks that change the texture of this chunk
       */
      registerListenerConditionally { (eventChunk, _): ChunkLoadedEvent ->
        if (eventChunk.isNeighbor(chunk)) {
          chunk.updateAllBlockLights()
        }
      }
    )
  }

  private inline fun <reified T : Event> registerListenerConditionally(listener: EventListener<T>): RegisteredEventListener =
    EventManager.registerListener<T> { event ->
      assertValid()
      listener.handle(event)
    }

  fun assertValid() {
    if (Settings.debug && chunk.isInvalid) {
      IllegalAction.LOG.handle { "Chunk must be valid, listened to events when this chunk is disposed" }
      dispose()
    }
  }

  override fun dispose() {
    listeners?.forEach(RegisteredEventListener::removeListener)
  }
}
