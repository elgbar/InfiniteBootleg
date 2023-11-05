package no.elg.infiniteBootleg.world.render.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.LongMap
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.events.chunks.ChunkTextureChangedEvent
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.util.ProgressHandler
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.safeUse
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.render.ClientWorldRender

class DebugChunkRenderer(private val worldRender: ClientWorldRender) : Renderer, Disposable {
  private val shapeRenderer: ShapeRenderer = ShapeRenderer(1000)
  private val camera: OrthographicCamera get() = worldRender.camera

  private val newlyUpdatedChunks = LongMap<ProgressHandler>()

  private val listener = EventManager.registerListener { e: ChunkTextureChangedEvent ->
    newlyUpdatedChunks.put(e.chunk.compactLocation, ProgressHandler(0.25f))
  }

  override fun render() {
    val chunksInView = worldRender.chunksInView
    val yEnd = chunksInView.verticalEnd
    val xEnd = chunksInView.horizontalEnd
    val worldBody = worldRender.world.worldBody
    val worldOffsetX = worldBody.worldOffsetX * Block.BLOCK_SIZE
    val worldOffsetY = worldBody.worldOffsetY * Block.BLOCK_SIZE
    val textureSize = Chunk.CHUNK_TEXTURE_SIZE

    if (Settings.renderChunkUpdates) {
      shapeRenderer.color = CHUNK_UPDATE_COLOR
      Gdx.gl.glEnable(GL30.GL_BLEND)
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
            shapeRenderer.rect(x * textureSize + 0.5f + worldOffsetX, y * textureSize + 0.5f + worldOffsetY, textureSize - 1f, textureSize - 1f)
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
              WITHIN_CAMERA_COLOR
            }
            shapeRenderer.rect(x * textureSize + 0.5f + worldOffsetX, y * textureSize + 0.5f + worldOffsetY, textureSize - 1f, textureSize - 1f)
          }
        }
      }
      val sr = ClientMain.inst().screenRenderer
      sr.batch.safeUse {
        sr.drawBottom("Debug Chunk outline legend", 5f)
        sr.font.color = WITHIN_CAMERA_COLOR
        sr.drawBottom("  Chunks within the camera boarders", 3f)
        sr.font.color = OUTSIDE_CAMERA_COLOR
        sr.drawBottom("  Chunks outside camera boarders, only physics active", 1f)
      }
      sr.resetFontColor()
    }
  }

  override fun dispose() {
    shapeRenderer.dispose()
    EventManager.removeListener(listener)
  }

  companion object {
    val WITHIN_CAMERA_COLOR: Color = Color.TEAL
    val OUTSIDE_CAMERA_COLOR: Color = Color.FIREBRICK
    val CHUNK_UPDATE_COLOR: Color = Color.GREEN
  }
}
