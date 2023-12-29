package no.elg.infiniteBootleg.world.chunks

import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.events.api.EventListener
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.events.chunks.ChunkLoadedEvent
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.worldToChunk

class ChunkColumnListeners : Disposable {

  /**
   * Make sure that top blocks of a chunk column is not below the assumed top block of the chunk column.
   *
   * There might be edge cases which this does not cover. But they should be fixed as they are found :)
   */
  private val validateChunkTopBlockOnChunkLoad: EventListener<ChunkLoadedEvent> = EventListener { (eventChunk, _): ChunkLoadedEvent ->
    val chunkColumn = eventChunk.chunkColumn
    for (localX in 0 until Chunk.CHUNK_SIZE) {
      if (chunkColumn.topBlockHeight(localX).worldToChunk() == eventChunk.chunkY) {
        Main.inst().scheduler.executeAsync {
          chunkColumn.updateTopBlock(localX)
        }
      }
    }
  }

  fun registerListeners() {
    EventManager.registerListener(validateChunkTopBlockOnChunkLoad)
  }

  override fun dispose() {
    EventManager.removeListener(validateChunkTopBlockOnChunkLoad)
  }
}
