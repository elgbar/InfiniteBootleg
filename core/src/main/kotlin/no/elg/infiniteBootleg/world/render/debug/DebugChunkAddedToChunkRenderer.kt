package no.elg.infiniteBootleg.world.render.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.LongMap
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.render.OverlayRenderer
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.events.chunks.ChunkAddedToChunkRendererEvent
import no.elg.infiniteBootleg.util.ProgressHandler
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.safeUse
import no.elg.infiniteBootleg.world.chunks.Chunk.Companion.CHUNK_TEXTURE_SIZE
import no.elg.infiniteBootleg.world.chunks.Chunk.Companion.CHUNK_TEXTURE_SIZE_HALF
import no.elg.infiniteBootleg.world.render.ClientWorldRender

class DebugChunkAddedToChunkRenderer(private val worldRender: ClientWorldRender) : OverlayRenderer, Disposable {
  private val shapeRenderer: ShapeRenderer = ShapeRenderer(1000)
  private val camera: OrthographicCamera get() = worldRender.camera

  private val newlyUpdatedChunks = LongMap<ToUpdateChunk>()

  private val listener = EventManager.registerListener { e: ChunkAddedToChunkRendererEvent ->
    if (isActive) {
      newlyUpdatedChunks.put(e.chunk.compactLocation, ToUpdateChunk(ProgressHandler(0.1f), e.prioritized))
    }
  }

  override val isActive: Boolean
    get() = Settings.renderChunkUpdates

  override fun render() {
    val chunksInView = worldRender.chunksInView
    val yEnd = chunksInView.verticalEnd
    val xEnd = chunksInView.horizontalEnd

    Gdx.gl.glEnable(GL30.GL_BLEND)
    val delta = Gdx.graphics.deltaTime
    shapeRenderer.safeUse(ShapeRenderer.ShapeType.Filled, camera.combined) {
      for (y in chunksInView.verticalStart - 1 until yEnd - 1) {
        for (x in chunksInView.horizontalStart - 1 until xEnd - 1) {
          val compactLoc = compactLoc(x, y)

          shapeRenderer.color = CHUNK_WILL_UPDATE_PRIORITY_COLOR
          val (updatedChunk, prioritized) = newlyUpdatedChunks.get(compactLoc) ?: continue

          shapeRenderer.color.a = updatedChunk.updateAndGetProgress(delta)
          if (updatedChunk.isDone()) {
            newlyUpdatedChunks.remove(compactLoc)
            continue
          }
          if (prioritized) {
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
          } else {
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
    listener.removeListener()
  }

  companion object {

    data class ToUpdateChunk(val progress: ProgressHandler, val prioritized: Boolean)

    val CHUNK_WILL_UPDATE_PRIORITY_COLOR: Color = Color.YELLOW
    val CHUNK_WILL_UPDATE_COLOR: Color = Color.RED
  }
}
