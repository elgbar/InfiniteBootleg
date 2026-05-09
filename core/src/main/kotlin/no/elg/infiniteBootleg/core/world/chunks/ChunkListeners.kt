package no.elg.infiniteBootleg.core.world.chunks

import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.events.BlockChangedEvent
import no.elg.infiniteBootleg.core.events.ChunkColumnUpdatedEvent
import no.elg.infiniteBootleg.core.events.api.Event
import no.elg.infiniteBootleg.core.events.api.EventListener
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.api.RegisteredEventListener
import no.elg.infiniteBootleg.core.events.chunks.ChunkLightChangedEvent
import no.elg.infiniteBootleg.core.events.chunks.ChunkLoadedEvent
import no.elg.infiniteBootleg.core.util.IllegalAction
import no.elg.infiniteBootleg.core.util.compactChunkToWorld
import no.elg.infiniteBootleg.core.util.isNeighbor
import no.elg.infiniteBootleg.core.util.isWithinRadius
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.queryEntities
import no.elg.infiniteBootleg.core.world.box2d.extensions.isAwake
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumn.Companion.FeatureFlag.isBlocksLightFlag

class ChunkListeners(private val chunk: ChunkImpl) : Disposable {

  private var listeners: List<RegisteredEventListener>? = null

  val chunkLookRange = (chunk.chunkX - 2)..(chunk.chunkX + 2)

  private fun onBlockChangedUpdateTexture(block: Block) {
    if (block.chunk != chunk && chunk is TexturedChunk) {
      // Update the texture of this chunk if a blocks changes either in this chunk or in a neighbor chunk
      if (chunk.isWithinRadius(block, 1f)) {
        chunk.queueForRendering(false)
      }
    }
  }

  private fun onBlockChangeAwakeBox2dBodies(block: Block) {
    if (block.chunk == chunk) {
      // Awakens players to allow them to jump in a hole when placing a block
      block.queryEntities { body, _ -> body.isAwake = true }
    }
  }

  fun registerListeners() {
    require(listeners == null) { "Listeners cannot be registered twice" }

    listeners = listOfNotNull(
      registerListenerConditionally { event: BlockChangedEvent ->
        // Note: there are multiple events registered in the same listener
        val block = event.oldOrNewBlock ?: return@registerListenerConditionally
        onBlockChangeAwakeBox2dBodies(block)
        onBlockChangedUpdateTexture(block)
      },

      /*
       * Register a location to be updated on the next chunk tick
       */
      registerListenerConditionally(chunk is TexturedChunk) { (chunkLoc, originLocalX, originLocalY): ChunkLightChangedEvent ->
        if (Settings.renderLight && chunk.isNeighbor(chunkLoc)) {
          (chunk as TexturedChunk).queueLightSource(compactChunkToWorld(chunkLoc, originLocalX, originLocalY))
        }
      },

      /*
       * Update chunk light when a chunk column is updated
       */
      registerListenerConditionally(chunk is TexturedChunk) { event: ChunkColumnUpdatedEvent ->
        if (Settings.renderLight && event.flag.isBlocksLightFlag() && event.chunkX in chunkLookRange) {
          (chunk as TexturedChunk).queueLightSources(event.calculatedDiffColumn)
        }
      },

      /*
       * When a neighbor chunk is loaded we might have to update the lights or the textures of this chunk since it might contain lights that
       * affect this chunk or the blocks that change the texture of this chunk
       */
      registerListenerConditionally(chunk is TexturedChunk) { (eventChunk, _): ChunkLoadedEvent ->
        if (eventChunk.isNeighbor(chunk)) {
          (chunk as TexturedChunk).updateAllBlockLights()
        }
      }
    )
  }

  private inline fun <reified T : Event> registerListenerConditionally(condition: Boolean = true, listener: EventListener<T>): RegisteredEventListener? =
    if (condition) {
      EventManager.registerListener<T> { event ->
        assertValid()
        listener.handle(event)
      }
    } else {
      null
    }

  fun assertValid() {
    if (Settings.debug && chunk.isInvalid) {
      IllegalAction.LOG.handle { "Chunk must be valid, listened to events when this chunk is disposed. Chunk $chunk" }
      dispose()
    }
  }

  override fun dispose() {
    listeners?.forEach(RegisteredEventListener::removeListener)
  }
}
