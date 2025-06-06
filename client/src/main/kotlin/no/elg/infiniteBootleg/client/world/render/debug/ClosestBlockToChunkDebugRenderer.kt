package no.elg.infiniteBootleg.client.world.render.debug

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.api.render.OverlayRenderer
import no.elg.infiniteBootleg.core.util.chunkToWorld
import no.elg.infiniteBootleg.core.util.closestBlockTo
import no.elg.infiniteBootleg.core.util.component1
import no.elg.infiniteBootleg.core.util.component2
import no.elg.infiniteBootleg.core.util.safeUse
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.BLOCK_TEXTURE_SIZE_F
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.getChunkOrNull

class ClosestBlockToChunkDebugRenderer(private val worldRender: ClientWorldRender) :
  OverlayRenderer,
  Disposable {

  private val shapeRenderer: ShapeRenderer = ShapeRenderer(1000).also {
    it.color = Color.RED
  }

  override val isActive: Boolean
    get() = Settings.renderClosestBlockToPlayerChunk

  override fun render() {
    shapeRenderer.safeUse(ShapeRenderer.ShapeType.Filled, worldRender.camera.combined) {
      val playerChunk = worldRender.world.playersEntities.firstOrNull()?.getChunkOrNull() ?: return
      val pointingAtBlock = ClientMain.inst().mouseLocator.mouseBlockCompactLoc
      val blockAt = worldRender.world.getBlock(pointingAtBlock) ?: return
      val closestBlockToChunk = playerChunk.closestBlockTo(blockAt)
      val (worldX, worldY) = playerChunk.compactLocation.chunkToWorld(closestBlockToChunk)

      shapeRenderer.rect(
        worldX * BLOCK_TEXTURE_SIZE_F + BLOCK_TEXTURE_SIZE_F / 4f,
        worldY * BLOCK_TEXTURE_SIZE_F + BLOCK_TEXTURE_SIZE_F / 4f,
        BLOCK_TEXTURE_SIZE_F / 2f,
        BLOCK_TEXTURE_SIZE_F / 2f
      )
    }
  }

  override fun dispose() {
    shapeRenderer.dispose()
  }
}
