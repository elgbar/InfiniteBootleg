package no.elg.infiniteBootleg.world.render.debug

import no.elg.infiniteBootleg.api.render.OverlayRenderer
import no.elg.infiniteBootleg.protobuf.EntityKt.texture
import no.elg.infiniteBootleg.util.withColor
import no.elg.infiniteBootleg.util.worldToScreen
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.render.ClientWorldRender
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion

abstract class SingleBlockDebugRenderer(private val worldRender: ClientWorldRender) : OverlayRenderer {

  protected open val alpha: Float = 0.25f
  protected abstract val texture: RotatableTextureRegion

  abstract fun shouldRender(block: Block): Boolean

  override fun render() {
    worldRender.batch.withColor(a = alpha) {
      for (chunk: Chunk in worldRender.world.loadedChunks) {
        for (block in chunk) {
          if (block != null && shouldRender(block)) {
            worldRender.batch.draw(
              texture.textureRegion,
              worldToScreen(block.worldX.toFloat()),
              worldToScreen(block.worldY.toFloat()),
              Block.BLOCK_SIZE.toFloat(),
              Block.BLOCK_SIZE.toFloat()
            )
          }
        }
      }
    }
  }
}
