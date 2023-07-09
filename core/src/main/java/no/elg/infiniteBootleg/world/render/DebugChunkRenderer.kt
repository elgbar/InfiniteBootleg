package no.elg.infiniteBootleg.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.LongMap
import ktx.graphics.use
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.events.chunks.ChunkTextureChangedEvent
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.Chunk

class DebugChunkRenderer(private val worldRender: ClientWorldRender) : Renderer, Disposable {
  private val lr: ShapeRenderer = ShapeRenderer(1000)
  private val camera: OrthographicCamera = worldRender.camera

  private val newlyUpdatedChunks = LongMap<UpdatedChunk>()

  private val listener = EventManager.registerListener({ e: ChunkTextureChangedEvent ->
    newlyUpdatedChunks.put(e.chunk.compactLocation, UpdatedChunk(e.chunk))
  })

  override fun render() {
    if (!Settings.renderChunkUpdates && !Settings.renderChunkBounds) {
      return
    }

    val chunksInView = worldRender.chunksInView
    val yEnd = chunksInView.verticalEnd
    val xEnd = chunksInView.horizontalEnd
    val worldBody = worldRender.world.worldBody
    val worldOffsetX = worldBody.worldOffsetX * Block.BLOCK_SIZE
    val worldOffsetY = worldBody.worldOffsetY * Block.BLOCK_SIZE
    val offset = Chunk.CHUNK_SIZE * Block.BLOCK_SIZE

    if (Settings.renderChunkBounds) {
      lr.use(ShapeRenderer.ShapeType.Line, camera.combined) {
        for (y in chunksInView.verticalStart until yEnd) {
          for (x in chunksInView.horizontalStart until xEnd) {
            val canNotSeeChunk = y == (chunksInView.verticalEnd - 1) || x == chunksInView.horizontalStart || x == (chunksInView.horizontalEnd - 1)
            lr.color = if (canNotSeeChunk) {
              OUTSIDE_CAMERA_COLOR
            } else {
              WITHIN_CAMERA_COLOR
            }
            lr.rect(x * offset + 0.5f + worldOffsetX, y * offset + 0.5f + worldOffsetY, offset - 1f, offset - 1f)
          }
        }
      }
    }
    if (Settings.renderChunkUpdates) {
      lr.use(ShapeRenderer.ShapeType.Filled, camera.combined) {
        for (y in chunksInView.verticalStart - 1 until yEnd - 1) {
          for (x in chunksInView.horizontalStart - 1 until xEnd - 1) {
            val compactLoc = compactLoc(x, y)
            val updatedChunk = newlyUpdatedChunks.get(compactLoc) ?: continue
            if (updatedChunk.isDone()) {
              newlyUpdatedChunks.remove(compactLoc)
              continue
            }
            val alpha = updatedChunk.calculateAlpha(Gdx.graphics.deltaTime)
            lr.color = FLASH_COLOR.apply { a = alpha }
            lr.rect(x * offset + 0.5f + worldOffsetX, y * offset + 0.5f + worldOffsetY, offset - 1f, offset - 1f)
          }
        }
      }
    }
    val sr = ClientMain.inst().screenRenderer
    sr.batch.use {
      sr.drawBottom("Debug Chunk outline legend", 5f)
      sr.font.color = WITHIN_CAMERA_COLOR
      sr.drawBottom("  Chunks within the camera boarders", 3f)
      sr.font.color = OUTSIDE_CAMERA_COLOR
      sr.drawBottom("  Chunks outside camera boarders, only physics active", 1f)
    }
    sr.resetFontColor()
  }

  override fun dispose() {
    lr.dispose()
    EventManager.registerListener(listener)
  }

  companion object {
    val WITHIN_CAMERA_COLOR: Color = Color.TEAL
    val OUTSIDE_CAMERA_COLOR: Color = Color.FIREBRICK
    val FLASH_COLOR: Color = Color.GREEN.cpy()
  }
}
