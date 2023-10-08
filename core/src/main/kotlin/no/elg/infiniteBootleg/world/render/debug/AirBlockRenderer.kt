package no.elg.infiniteBootleg.world.render.debug

import no.elg.infiniteBootleg.Settings.renderAirBlocks
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.withColor
import no.elg.infiniteBootleg.util.worldToScreen
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.render.ClientWorldRender

class AirBlockRenderer(private val worldRender: ClientWorldRender) : Renderer {

  private val assets get() = Main.inst().assets
  private val worldBody = worldRender.world.worldBody

  override fun render() {
    if (renderAirBlocks) {
      worldRender.batch.withColor(a = 0.25f) {
        for (chunk in worldRender.world.loadedChunks) {
          for (block in chunk) {
            if (block?.material == Material.AIR) {
              renderAirBlock(block)
            }
          }
        }
      }
    }
  }

  private fun renderAirBlock(airBlock: Block) {
    worldRender.batch.draw(
      assets.visibleAirTexture.textureRegion,
      worldToScreen(airBlock.worldX.toFloat(), worldBody.worldOffsetX),
      worldToScreen(airBlock.worldY.toFloat(), worldBody.worldOffsetY),
      Block.BLOCK_SIZE.toFloat(),
      Block.BLOCK_SIZE.toFloat()
    )
  }
}
