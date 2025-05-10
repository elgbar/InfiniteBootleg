package no.elg.infiniteBootleg.client.world.render.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.LongMap
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.api.render.OverlayRenderer
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.chunks.ChunkTextureChangedEvent
import no.elg.infiniteBootleg.core.util.ProgressHandler
import no.elg.infiniteBootleg.core.util.compactLoc
import no.elg.infiniteBootleg.core.util.safeUse
import no.elg.infiniteBootleg.core.world.chunks.Chunk.Companion.CHUNK_TEXTURE_SIZE

class DebugChunkRenderer(private val worldRender: ClientWorldRender) :
  OverlayRenderer,
  Disposable {
  private val shapeRenderer: ShapeRenderer = ShapeRenderer(1000)
  private val camera: OrthographicCamera get() = worldRender.camera

  private val newlyUpdatedChunks = LongMap<ProgressHandler>()

  private val listener = EventManager.registerListener { e: ChunkTextureChangedEvent ->
    newlyUpdatedChunks.put(e.chunkLoc, ProgressHandler(0.25f))
  }

  override val isActive: Boolean
    get() = Settings.renderChunkUpdates || Settings.renderChunkBounds

  override fun render() {
    val chunksInView = worldRender.chunksInView
    val yEnd = chunksInView.verticalEnd
    val xEnd = chunksInView.horizontalEnd

    if (Settings.renderChunkUpdates) {
      shapeRenderer.color = CHUNK_UPDATE_COLOR
      shapeRenderer.safeUse(ShapeRenderer.ShapeType.Filled, camera.combined) {
        for (y in chunksInView.verticalStart - 1 until yEnd - 1) {
          for (x in chunksInView.horizontalStart - 1 until xEnd - 1) {
            val compactLoc = compactLoc(x, y)
            val updatedChunk = newlyUpdatedChunks.get(compactLoc) ?: continue
            shapeRenderer.color.a = updatedChunk.updateAndGetProgress(Gdx.graphics.deltaTime)
            if (updatedChunk.isDone()) {
              newlyUpdatedChunks.remove(compactLoc)
              continue
            }
            shapeRenderer.rect(x * CHUNK_TEXTURE_SIZE + 0.5f, y * CHUNK_TEXTURE_SIZE + 0.5f, CHUNK_TEXTURE_SIZE - 1f, CHUNK_TEXTURE_SIZE - 1f)
          }
        }
      }
    }
    if (Settings.renderChunkBounds) {
      shapeRenderer.safeUse(ShapeRenderer.ShapeType.Line, camera.combined) {
        for (y in chunksInView.verticalStart until yEnd) {
          for (x in chunksInView.horizontalStart until xEnd) {
            val canNotSeeChunk = y == (chunksInView.verticalEnd - 1) || x == chunksInView.horizontalStart || x == (chunksInView.horizontalEnd - 1)
            shapeRenderer.color = if (canNotSeeChunk) {
              OUTSIDE_CAMERA_COLOR
            } else {
              if (worldRender.world.isChunkLoaded(x, y)) {
                WITHIN_CAMERA_COLOR
              } else {
                NOT_LOADED_COLOR
              }
            }
            shapeRenderer.rect(x * CHUNK_TEXTURE_SIZE + 0.5f, y * CHUNK_TEXTURE_SIZE + 0.5f, CHUNK_TEXTURE_SIZE - 1f, CHUNK_TEXTURE_SIZE - 1f)
          }
        }
      }
      ClientMain.inst().screenRenderer.use { sr ->
        sr.drawBottom("Debug Chunk outline legend", 7f)
        sr.font.color = WITHIN_CAMERA_COLOR
        sr.drawBottom("  Loaded chunks within the camera boarders", 5f)
        sr.font.color = NOT_LOADED_COLOR
        sr.drawBottom("  Unloaded chunks within the camera boarders", 3f)
        sr.font.color = OUTSIDE_CAMERA_COLOR
        sr.drawBottom("  Chunks outside camera boarders, only physics active", 1f)
      }
    }
  }

  override fun dispose() {
    shapeRenderer.dispose()
    listener.removeListener()
  }

  companion object {
    val WITHIN_CAMERA_COLOR: Color = Color.TEAL
    val OUTSIDE_CAMERA_COLOR: Color = Color.ORANGE
    val NOT_LOADED_COLOR: Color = Color.FIREBRICK
    val CHUNK_UPDATE_COLOR: Color = Color.GREEN
  }
}
