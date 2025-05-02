package no.elg.infiniteBootleg.client.world.render.debug

import com.badlogic.gdx.graphics.g2d.Batch
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.api.render.OverlayRenderer
import no.elg.infiniteBootleg.core.util.withColor
import no.elg.infiniteBootleg.core.util.worldToScreen
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion

abstract class SingleBlockDebugRenderer(private val worldRender: ClientWorldRender) : OverlayRenderer {

  protected open val alpha: Float = 0.25f
  protected abstract val texture: RotatableTextureRegion

  abstract fun shouldRender(block: Block): Boolean
  open fun beforeRender(block: Block, batch: Batch): Unit = Unit

  override fun render() {
    worldRender.batch.withColor(a = alpha) { batch ->
      for (chunk: Chunk in worldRender.world.loadedChunks) {
        for (block in chunk) {
          if (block != null && shouldRender(block)) {
            beforeRender(block, batch)
            batch.draw(
              texture.textureRegion,
              block.worldX.toFloat().worldToScreen(),
              block.worldY.toFloat().worldToScreen(),
              Block.BLOCK_TEXTURE_SIZE_F,
              Block.BLOCK_TEXTURE_SIZE_F
            )
          }
        }
      }
    }
  }
}
