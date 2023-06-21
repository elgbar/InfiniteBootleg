package no.elg.infiniteBootleg.world.render

import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.util.withColor
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.ecs.components.SelectedMaterialComponent.Companion.selectedMaterial
import no.elg.infiniteBootleg.world.ecs.selectedMaterialComponentFamily
import java.lang.Math.floorMod
import kotlin.math.floor

class HoveringBlockRenderer(private val worldRender: ClientWorldRender) : Renderer {

  override fun render() {
    val mouseLocator = ClientMain.inst().mouseLocator
    for (entity in worldRender.world.engine.getEntitiesFor(selectedMaterialComponentFamily)) {
      val texture = entity.selectedMaterial.material.textureRegion ?: continue

      if (worldRender.world.canPlaceBlock(mouseLocator.mouseBlockX, mouseLocator.mouseBlockY)) {
        val averageBrightness = worldRender.world.getBlockLight(mouseLocator.mouseBlockX, mouseLocator.mouseBlockY)?.averageBrightness ?: 1f
        if (averageBrightness == 0f) {
          // no need to render a black block
          continue
        }
        val a = (1f - averageBrightness).coerceAtLeast(0.33f)
        worldRender.batch.withColor(averageBrightness, averageBrightness, averageBrightness, a) {
          val mouseScreenX = floor(mouseLocator.screenInputVec.x).toInt()
          val mouseScreenY = floor(mouseLocator.screenInputVec.y).toInt()
          val diffFromBlockSizeX = floorMod(mouseScreenX, Block.BLOCK_SIZE).toFloat()
          val diffFromBlockSizeY = floorMod(mouseScreenY, Block.BLOCK_SIZE).toFloat()
          it.draw(
            texture,
            // Draw the block aligned to the block grid
            mouseScreenX - diffFromBlockSizeX,
            mouseScreenY - diffFromBlockSizeY,
            0f,
            0f,
            Block.BLOCK_SIZE.toFloat(),
            Block.BLOCK_SIZE.toFloat(),
            1f,
            1f,
            0f
          )
        }
      }
    }
  }
}
