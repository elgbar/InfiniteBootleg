package no.elg.infiniteBootleg.client.world.render.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.api.render.OverlayRenderer
import no.elg.infiniteBootleg.core.events.LeafDecayCheckEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.util.ProgressHandler
import no.elg.infiniteBootleg.core.util.component1
import no.elg.infiniteBootleg.core.util.component2
import no.elg.infiniteBootleg.core.util.safeUse
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.BLOCK_TEXTURE_SIZE_F

class LeafDecayDebugRenderer(private val worldRender: ClientWorldRender) : OverlayRenderer, Disposable {

  private val shapeRenderer: ShapeRenderer = ShapeRenderer(1000).also {
    it.color = LEAF_DECAY_CHECK_SRC_COLOR
  }

  private val srcBlocks = Long2ObjectOpenHashMap<ProgressHandler>()
  private val seenBlocks = Long2ObjectOpenHashMap<ProgressHandler>()

  private val listener = EventManager.registerListener { e: LeafDecayCheckEvent ->
    if (isActive) {
      srcBlocks.put(e.compactBlockLoc, ProgressHandler(1f))
      e.seen.longIterator().forEachRemaining { seenBlocks.put(it, ProgressHandler(.5f)) }
    } else {
      srcBlocks.clear()
      seenBlocks.clear()
    }
  }

  override val isActive: Boolean
    get() = Settings.debug && Settings.renderLeafDecay

  private fun render(locs: Long2ObjectMap<ProgressHandler>, color: Color) {
    val delta = Gdx.graphics.deltaTime
    locs.long2ObjectEntrySet().removeIf { it.value.update(delta) }

    for ((compactLoc, visualizeUpdate: ProgressHandler) in locs.long2ObjectEntrySet()) {
      val (worldX, worldY) = compactLoc
      shapeRenderer.color = color
      shapeRenderer.color.a = visualizeUpdate.progress
      shapeRenderer.rect(
        worldX * BLOCK_TEXTURE_SIZE_F + BLOCK_TEXTURE_SIZE_F / 4f,
        worldY * BLOCK_TEXTURE_SIZE_F + BLOCK_TEXTURE_SIZE_F / 4f,
        BLOCK_TEXTURE_SIZE_F / 2f,
        BLOCK_TEXTURE_SIZE_F / 2f
      )
    }
  }

  override fun render() {
    if (srcBlocks.isNotEmpty() || seenBlocks.isNotEmpty()) {
      Gdx.gl.glEnable(GL30.GL_BLEND)
      shapeRenderer.safeUse(ShapeRenderer.ShapeType.Filled, worldRender.camera.combined) {
        render(srcBlocks, LEAF_DECAY_CHECK_SRC_COLOR)
        render(seenBlocks, LEAF_DECAY_CHECK_SEEN_COLOR)
      }
    }
  }

  override fun dispose() {
    shapeRenderer.dispose()
    listener.removeListener()
  }

  companion object {
    val LEAF_DECAY_CHECK_SRC_COLOR: Color = Color.NAVY
    val LEAF_DECAY_CHECK_SEEN_COLOR: Color = Color.ROYAL
  }
}
