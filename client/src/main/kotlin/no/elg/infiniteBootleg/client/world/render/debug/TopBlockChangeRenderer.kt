package no.elg.infiniteBootleg.client.world.render.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.IntMap
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.api.render.OverlayRenderer
import no.elg.infiniteBootleg.core.events.ChunkColumnUpdatedEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.util.ProgressHandler
import no.elg.infiniteBootleg.core.util.blend
import no.elg.infiniteBootleg.core.util.chunkToWorld
import no.elg.infiniteBootleg.core.util.compactInt
import no.elg.infiniteBootleg.core.util.component1
import no.elg.infiniteBootleg.core.util.component2
import no.elg.infiniteBootleg.core.util.safeUse
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.BLOCK_TEXTURE_SIZE_F
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumn.Companion.FeatureFlag.SOLID_FLAG
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumn.Companion.FeatureFlag.chunkColumnFeatureFlags
import java.util.concurrent.ConcurrentHashMap

class TopBlockChangeRenderer(private val worldRender: ClientWorldRender) :
  OverlayRenderer,
  Disposable {

  private val shapeRenderer: ShapeRenderer = ShapeRenderer(1000)
  private val camera: OrthographicCamera get() = worldRender.camera

  private val newlyUpdatedChunks = ConcurrentHashMap<Long, ChunkColumnUpdate>()

  override val isActive: Boolean
    get() = (Settings.renderTopBlockChanges && newlyUpdatedChunks.isNotEmpty()) || Settings.renderTopBlocks

  data class ChunkColumnUpdate(
    val progress: ProgressHandler,
    val index: Long,
    val worldX: Float,
    val worldNewY: Float,
    val worldOldY: Float,
    val flagSetting: FlagSetting,
    val diff: LongArray
  )

  data class FlagSetting(
    val newColor: Color,
    val oldColor: Color,
    val width: Float = BLOCK_TEXTURE_SIZE_F,
    val height: Float = BLOCK_TEXTURE_SIZE_F,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
  )

  private val flagSettings = IntMap<FlagSetting>().apply {
    put(BLOCKS_LIGHT_FLAG, FlagSetting(NEW_LIGHT_COLOR, OLD_LIGHT_COLOR, height = BLOCK_TEXTURE_SIZE_F / 2f))
    put(SOLID_FLAG, FlagSetting(NEW_SOLID_COLOR, OLD_SOLID_COLOR, height = BLOCK_TEXTURE_SIZE_F / 2f, offsetY = BLOCK_TEXTURE_SIZE_F / 2f))
  }

  private val listener = EventManager.registerListener { e: ChunkColumnUpdatedEvent ->
    if (Settings.renderTopBlockChanges) {
      for (flag in chunkColumnFeatureFlags) {
        if (e.flag and flag != 0) {
          val index = compactInt(e.chunkX.chunkToWorld(e.localX), flag)
          val flagSetting = flagSettings[flag] ?: continue
          val chunkColumnUpdate = ChunkColumnUpdate(
            progress = ProgressHandler(2f),
            index = index,
            worldX = e.chunkX.chunkToWorld(e.localX).toFloat(),
            worldNewY = e.newTopWorldY.toFloat(),
            worldOldY = e.oldTopWorldY.toFloat(),
            flagSetting = flagSetting,
            diff = e.calculatedDiffColumn
          )
          newlyUpdatedChunks[index] = chunkColumnUpdate
        }
      }
    }
  }

  override fun render() {
    shapeRenderer.safeUse(ShapeRenderer.ShapeType.Filled, camera.combined) {
      for ((progressHandler, index, worldX, worldNewY, worldOldY, flagSetting, diff) in newlyUpdatedChunks.values) {
        val progress = progressHandler.updateAndGetProgress(Gdx.graphics.deltaTime)
        if (progressHandler.isDone()) {
          newlyUpdatedChunks.remove(index)
          continue
        }

        fun renderBlock(color: Color, worldX: Float, worldY: Float) {
          shapeRenderer.color.set(color)
          shapeRenderer.color.a = progress
          shapeRenderer.rect(
            worldX * BLOCK_TEXTURE_SIZE_F + flagSetting.offsetX,
            worldY * BLOCK_TEXTURE_SIZE_F + flagSetting.offsetY,
            flagSetting.width,
            flagSetting.height
          )
        }

        val color = Color(flagSetting.oldColor)
        val size = diff.size.toFloat()
        for ((i, pos) in diff.withIndex()) {
          val (x, y) = pos
          val colorProgress = i / size
          color.blend(flagSetting.oldColor, flagSetting.newColor, colorProgress)
          renderBlock(color, x.toFloat(), y.toFloat())
        }

        // Rendering the changed block twice will highlight the changes
        renderBlock(flagSetting.newColor, worldX, worldNewY)
        renderBlock(flagSetting.oldColor, worldX, worldOldY)
      }
    }
  }

  override fun dispose() {
    shapeRenderer.dispose()
    listener.removeListener()
  }

  companion object {
    val OLD_LIGHT_COLOR = Color(.7f, .1f, .9f, 1f)
    val NEW_LIGHT_COLOR = Color(.9f, .9f, 0.2f, 1f)

    val OLD_SOLID_COLOR = Color(.8f, .2f, .2f, 1f)
    val NEW_SOLID_COLOR = Color(.2f, .9f, 0.5f, 1f)
  }
}
