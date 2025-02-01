package no.elg.infiniteBootleg.core.world.chunks

import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.chunks.ChunkLoadedEvent
import no.elg.infiniteBootleg.core.util.launchOnAsync
import no.elg.infiniteBootleg.core.util.worldToChunk

class ChunkColumnListeners : Disposable {

  /**
   * Make sure that top blocks of a chunk column is not below the assumed top block of the chunk column.
   *
   * There might be edge cases which this does not cover. But they should be fixed as they are found :)
   */
  private val validateChunkTopBlockOnChunkLoad = EventManager.registerListener { (eventChunk, _): ChunkLoadedEvent ->
    val chunkColumn = eventChunk.chunkColumn
    for (localX in 0 until Chunk.CHUNK_SIZE) {
      if (chunkColumn.topBlockHeight(localX).worldToChunk() == eventChunk.chunkY) {
        launchOnAsync {
          chunkColumn.updateTopBlock(localX)
        }
      }
    }
  }

  override fun dispose() {
    validateChunkTopBlockOnChunkLoad.removeListener()
  }
}
