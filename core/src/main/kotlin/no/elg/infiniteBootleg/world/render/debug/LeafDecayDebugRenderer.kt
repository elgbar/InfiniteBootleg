package no.elg.infiniteBootleg.world.render.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.LongMap
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.render.OverlayRenderer
import no.elg.infiniteBootleg.events.LeafDecayCheckEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.util.LongMapUtil.component1
import no.elg.infiniteBootleg.util.LongMapUtil.component2
import no.elg.infiniteBootleg.util.ProgressHandler
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.util.safeUse
import no.elg.infiniteBootleg.world.blocks.Block.Companion.BLOCK_SIZE
import no.elg.infiniteBootleg.world.render.ClientWorldRender

class LeafDecayDebugRenderer(private val worldRender: ClientWorldRender) : OverlayRenderer, Disposable {

  private val shapeRenderer: ShapeRenderer = ShapeRenderer(1000).also {
    it.color = LEAF_DECAY_CHECK_COLOR
  }

  private val newlyUpdatedChunks = LongMap<ProgressHandler>()
  private val listener = EventManager.registerListener { e: LeafDecayCheckEvent ->
    if (Settings.renderLeafDecay) {
      newlyUpdatedChunks.put(e.compactBlockLoc, ProgressHandler(2f))
    }
  }

  override val isActive: Boolean
    get() = Settings.renderLeafDecay

  override fun render() {
    newlyUpdatedChunks.removeAll { (_, it: ProgressHandler?) -> it == null || it.isDone() }
    Gdx.gl.glEnable(GL30.GL_BLEND)
    shapeRenderer.safeUse(ShapeRenderer.ShapeType.Filled, worldRender.camera.combined) {
      for ((compactLoc, visualizeUpdate: ProgressHandler?) in newlyUpdatedChunks.entries()) {
        val (worldX, worldY) = compactLoc
        shapeRenderer.color.a = visualizeUpdate?.updateAndGetProgress(Gdx.graphics.deltaTime) ?: continue
        shapeRenderer.rect(worldX * TEXTURE_SIZE + TEXTURE_SIZE / 4f, worldY * TEXTURE_SIZE + TEXTURE_SIZE / 4f, TEXTURE_SIZE / 2f, TEXTURE_SIZE / 2f)
      }
    }
  }

  override fun dispose() {
    shapeRenderer.dispose()
    listener.removeListener()
  }

  companion object {
    val LEAF_DECAY_CHECK_COLOR: Color = Color.NAVY
    const val TEXTURE_SIZE = BLOCK_SIZE.toFloat()
  }
}
