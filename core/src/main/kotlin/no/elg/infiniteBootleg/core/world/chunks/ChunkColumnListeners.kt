package no.elg.infiniteBootleg.core.world.chunks

import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.chunks.ChunkLoadedEvent
import no.elg.infiniteBootleg.core.util.launchOnAsync
import no.elg.infiniteBootleg.core.util.worldToChunk
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumn.Companion.FeatureFlag

class ChunkColumnListeners : Disposable {

  /**
   * Make sure that top blocks of a chunk column is not below the assumed top block of the chunk column.
   *
   * There might be edge cases which this does not cover. But they should be fixed as they are found :)
   */
  private val validateChunkTopBlockOnChunkLoad = EventManager.registerListener { (eventChunk, _): ChunkLoadedEvent ->
    val chunkColumn = eventChunk.chunkColumn
    for (localX in 0 until Chunk.CHUNK_SIZE) {
      val worldToChunkLight = chunkColumn.topBlockHeight(localX, FeatureFlag.BLOCKS_LIGHT_FLAG).worldToChunk()
      val worldToChunkSolid = chunkColumn.topBlockHeight(localX, FeatureFlag.SOLID_FLAG).worldToChunk()
      if (worldToChunkLight == eventChunk.chunkY || worldToChunkSolid == eventChunk.chunkY) {
        val flagLight = if (worldToChunkLight == eventChunk.chunkY) FeatureFlag.BLOCKS_LIGHT_FLAG else 0
        val flagSolid = if (worldToChunkSolid == eventChunk.chunkY) FeatureFlag.SOLID_FLAG else 0
        launchOnAsync {
          chunkColumn.updateTopBlockWithoutHint(localX, flagLight or flagSolid)
        }
      }
    }
  }

  override fun dispose() {
    validateChunkTopBlockOnChunkLoad.removeListener()
  }
}
