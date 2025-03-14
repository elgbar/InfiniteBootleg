package no.elg.infiniteBootleg.core.world.chunks

import com.badlogic.gdx.utils.Disposable
import io.github.oshai.kotlinlogging.KotlinLogging
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.events.BlockChangedEvent
import no.elg.infiniteBootleg.core.events.ChunkColumnUpdatedEvent
import no.elg.infiniteBootleg.core.events.WorldTickedEvent
import no.elg.infiniteBootleg.core.events.api.Event
import no.elg.infiniteBootleg.core.events.api.EventListener
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.api.RegisteredEventListener
import no.elg.infiniteBootleg.core.events.chunks.ChunkLightChangedEvent
import no.elg.infiniteBootleg.core.events.chunks.ChunkLoadedEvent
import no.elg.infiniteBootleg.core.util.IllegalAction
import no.elg.infiniteBootleg.core.util.WorldCompactLocArray
import no.elg.infiniteBootleg.core.util.compactChunkToWorld
import no.elg.infiniteBootleg.core.util.isNeighbor
import no.elg.infiniteBootleg.core.util.isWithinRadius
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.queryEntities
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumn.Companion.FeatureFlag.isBlocksLightFlag

private val logger = KotlinLogging.logger {}

class ChunkListeners(private val chunk: ChunkImpl) : Disposable {

  private var listeners: Array<RegisteredEventListener>? = null
  private val lightLocs = LongOpenHashSet(0)

  val chunkLookRange = (chunk.chunkX - 2)..(chunk.chunkX + 2)
  val chunkCompactLocation = chunk.compactLocation

  fun registerListeners() {
    require(listeners == null) { "Listeners cannot be registered twice" }

    listeners = arrayOf(
      registerListenerConditionally { event: BlockChangedEvent ->
        // Note: there are two events registered in the same listener
        val block = event.oldOrNewBlock ?: return@registerListenerConditionally
        if (block.chunk === chunk) {
          // Awakens players to allow them to jump in a hole when placing a block
          block.queryEntities {
            for ((body, _) in it) {
              body.isAwake = true
            }
          }
        } else if (chunk is TexturedChunk) {
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
          if (sources.isEmpty()) return@registerListenerConditionally // Fast, unsynchronized return
          val sourcesArray = synchronized(sources) {
            sources.toLongArray().also {
              sources.clear()
            }
          }
          chunk.doUpdateLightMultipleSources(sourcesArray, checkDistance = true)
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
        if (event.flag.isBlocksLightFlag() && event.chunkX in chunkLookRange) {
          val lights: WorldCompactLocArray = event.calculatedDiffColumn
          if (lights.isEmpty()) return@registerListenerConditionally // Fast, unsynchronized return
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
