package no.elg.infiniteBootleg.world.render.debug

import no.elg.infiniteBootleg.Settings.debugEntityMarkerBlocks
import no.elg.infiniteBootleg.api.render.OverlayRenderer
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.isMarkerBlock
import no.elg.infiniteBootleg.util.withColor
import no.elg.infiniteBootleg.util.worldToScreen
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.render.ClientWorldRender

class EntityMarkerBlockRenderer(private val worldRender: ClientWorldRender) : OverlayRenderer {

  private val assets get() = Main.inst().assets

  override val isActive: Boolean get() = debugEntityMarkerBlocks

  override fun render() {
    worldRender.batch.withColor(a = 0.25f) {
      for (chunk in worldRender.world.loadedChunks) {
        for (block in chunk) {
          if (block.isMarkerBlock()) {
            renderAirBlock(block)
          }
        }
      }
    }
  }

  private fun renderAirBlock(airBlock: Block) {
    worldRender.batch.draw(
      assets.handTexture.textureRegion,
      worldToScreen(airBlock.worldX.toFloat()),
      worldToScreen(airBlock.worldY.toFloat()),
      Block.BLOCK_SIZE.toFloat(),
      Block.BLOCK_SIZE.toFloat()
    )
  }
}
