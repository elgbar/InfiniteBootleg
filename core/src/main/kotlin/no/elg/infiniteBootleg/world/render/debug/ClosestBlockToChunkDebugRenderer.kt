package no.elg.infiniteBootleg.world.render.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.render.OverlayRenderer
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.util.chunkToWorld
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.util.safeUse
import no.elg.infiniteBootleg.util.closestBlockTo
import no.elg.infiniteBootleg.world.blocks.Block.Companion.BLOCK_SIZE
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.getChunkOrNull
import no.elg.infiniteBootleg.world.render.ClientWorldRender

class ClosestBlockToChunkDebugRenderer(private val worldRender: ClientWorldRender) : OverlayRenderer, Disposable {

  private val shapeRenderer: ShapeRenderer = ShapeRenderer(1000).also {
    it.color = Color.RED
  }

  override val isActive: Boolean
    get() = Settings.renderClosestBlockToPlayerChunk

  override fun render() {
    Gdx.gl.glEnable(GL30.GL_BLEND)
    shapeRenderer.safeUse(ShapeRenderer.ShapeType.Filled, worldRender.camera.combined) {
      val playerChunk = worldRender.world.playersEntities.firstOrNull()?.getChunkOrNull() ?: return
      val pointingAtBlock = ClientMain.inst().mouseLocator.mouseBlockCompactLoc
      val blockAt = worldRender.world.getBlock(pointingAtBlock) ?: return
      val closestBlockToChunk = playerChunk.closestBlockTo(blockAt)
      val (worldX, worldY) = playerChunk.compactLocation.chunkToWorld(closestBlockToChunk)

      shapeRenderer.rect(worldX * TEXTURE_SIZE + TEXTURE_SIZE / 4f, worldY * TEXTURE_SIZE + TEXTURE_SIZE / 4f, TEXTURE_SIZE / 2f, TEXTURE_SIZE / 2f)
    }
  }

  override fun dispose() {
    shapeRenderer.dispose()
  }

  companion object {
    const val TEXTURE_SIZE = BLOCK_SIZE.toFloat()
  }
}
