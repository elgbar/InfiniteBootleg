package no.elg.infiniteBootleg.world.render

import com.badlogic.gdx.graphics.g2d.TextureRegion
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.util.decompactLocX
import no.elg.infiniteBootleg.util.decompactLocY
import no.elg.infiniteBootleg.util.placeableBlock
import no.elg.infiniteBootleg.util.withColor
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledOrNull
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent.Companion.selected
import no.elg.infiniteBootleg.world.ecs.selectedMaterialComponentFamily
import no.elg.infiniteBootleg.world.world.World
import java.lang.Math.floorMod

class HoveringBlockRenderer(private val worldRender: ClientWorldRender) : Renderer {

  override fun render() {
    if (ClientMain.inst().shouldIgnoreWorldInput()) {
      return
    }
    val mouseLocator = ClientMain.inst().mouseLocator
    val world = worldRender.world
    for (entity in world.engine.getEntitiesFor(selectedMaterialComponentFamily)) {
      val texture = entity.selected.material.textureRegion ?: continue

      val keyboardControls = entity.locallyControlledOrNull?.keyboardControls ?: continue
      entity.placeableBlock(world, mouseLocator.mouseBlockX, mouseLocator.mouseBlockY, keyboardControls.brushSize, keyboardControls.interactRadius)
        .forEach { compactLoc ->
          val blockWorldX = compactLoc.decompactLocX()
          val blockWorldY = compactLoc.decompactLocY()
          renderBlock(world, texture, blockWorldX, blockWorldY)
        }
    }
  }

  private fun renderBlock(world: World, texture: TextureRegion, blockWorldX: Int, blockWorldY: Int) {
    val averageBrightness = world.getBlockLight(blockWorldX, blockWorldY)?.averageBrightness ?: 1f
    if (averageBrightness == 0f) {
      // no need to render a black block
      return
    }
    val a = (1f - averageBrightness).coerceAtLeast(0.33f)
    worldRender.batch.withColor(averageBrightness, averageBrightness, averageBrightness, a) {
      val mouseScreenX = blockWorldX * Block.BLOCK_SIZE
      val mouseScreenY = blockWorldY * Block.BLOCK_SIZE
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
