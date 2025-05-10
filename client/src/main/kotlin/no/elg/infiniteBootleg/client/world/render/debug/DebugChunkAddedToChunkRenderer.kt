package no.elg.infiniteBootleg.client.world.render.debug

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.google.errorprone.annotations.concurrent.GuardedBy
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.api.render.OverlayRenderer
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.chunks.ChunkAddedToChunkRendererEvent
import no.elg.infiniteBootleg.core.events.chunks.ChunkTextureChangeRejectedEvent
import no.elg.infiniteBootleg.core.events.chunks.ChunkTextureChangedEvent
import no.elg.infiniteBootleg.core.util.compactLoc
import no.elg.infiniteBootleg.core.util.safeUse
import no.elg.infiniteBootleg.core.world.chunks.Chunk.Companion.CHUNK_TEXTURE_SIZE
import no.elg.infiniteBootleg.core.world.chunks.Chunk.Companion.CHUNK_TEXTURE_SIZE_HALF

class DebugChunkAddedToChunkRenderer(private val worldRender: ClientWorldRender) :
  OverlayRenderer,
  Disposable {
  private val shapeRenderer: ShapeRenderer = ShapeRenderer(1000)
  private val camera: OrthographicCamera get() = worldRender.camera

  @GuardedBy("itself")
  private val newlyUpdatedChunks = Long2ByteOpenHashMap().also { it.defaultReturnValue(NOT_QUEUED) }

  private val listenerAddedToChunkRenderer = EventManager.registerListener { e: ChunkAddedToChunkRendererEvent ->
    if (isActive) {
      synchronized(newlyUpdatedChunks) {
        newlyUpdatedChunks.put(e.chunkLoc, if (e.prioritized) PRIORITIZED else NOT_PRIORITIZED)
      }
    } else {
      newlyUpdatedChunks.clear()
    }
  }

  private val listenerTextureChanged = EventManager.registerListener { e: ChunkTextureChangedEvent ->
    // Remove event when not active
    synchronized(newlyUpdatedChunks) { newlyUpdatedChunks.remove(e.chunkLoc) }
  }
  private val listenerTextureChangeRejected = EventManager.registerListener { e: ChunkTextureChangeRejectedEvent ->
    // Remove event when not active
    synchronized(newlyUpdatedChunks) { newlyUpdatedChunks.remove(e.chunkLoc) }
  }

  override val isActive: Boolean
    get() = Settings.renderChunkUpdates

  override fun render() {
    val chunksInView = worldRender.chunksInView
    val yEnd = chunksInView.verticalEnd
    val xEnd = chunksInView.horizontalEnd

    shapeRenderer.safeUse(ShapeRenderer.ShapeType.Filled, camera.combined) {
      for (y in chunksInView.verticalStart - 1 until yEnd - 1) {
        for (x in chunksInView.horizontalStart - 1 until xEnd - 1) {
          val compactLoc = compactLoc(x, y)

          val prioritization = newlyUpdatedChunks.get(compactLoc) // Get should be safe since we are not removing any elements

          if (prioritization == PRIORITIZED) {
            shapeRenderer.color = CHUNK_WILL_UPDATE_PRIORITY_COLOR
            shapeRenderer.rect(
              /* x = */
              x * CHUNK_TEXTURE_SIZE + 0.5f,
              /* y = */
              y * CHUNK_TEXTURE_SIZE + 0.5f,
              /* width = */
              CHUNK_TEXTURE_SIZE_HALF - 1f,
              /* height = */
              CHUNK_TEXTURE_SIZE_HALF - 1f
            )
          } else if (prioritization == NOT_PRIORITIZED) {
            shapeRenderer.color = CHUNK_WILL_UPDATE_COLOR
            shapeRenderer.rect(
              /* x = */
              x * CHUNK_TEXTURE_SIZE + 0.5f + CHUNK_TEXTURE_SIZE_HALF,
              /* y = */
              y * CHUNK_TEXTURE_SIZE + 0.5f + CHUNK_TEXTURE_SIZE_HALF,
              /* width = */
              CHUNK_TEXTURE_SIZE_HALF - 1f,
              /* height = */
              CHUNK_TEXTURE_SIZE_HALF - 1f
            )
          }
        }
      }
    }
  }

  override fun dispose() {
    shapeRenderer.dispose()
    listenerAddedToChunkRenderer.removeListener()
    listenerTextureChanged.removeListener()
    listenerTextureChangeRejected.removeListener()
  }

  companion object {

    private const val PRIORITIZED = 2.toByte()
    private const val NOT_PRIORITIZED = 1.toByte()
    private const val NOT_QUEUED = 0.toByte()

    val CHUNK_WILL_UPDATE_PRIORITY_COLOR: Color = Color.YELLOW
    val CHUNK_WILL_UPDATE_COLOR: Color = Color.RED
  }
}
