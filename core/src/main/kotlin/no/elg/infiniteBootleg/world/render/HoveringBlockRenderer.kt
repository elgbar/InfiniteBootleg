package no.elg.infiniteBootleg.world.render

import com.badlogic.gdx.graphics.g2d.TextureRegion
import no.elg.infiniteBootleg.KAssets
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.breakableBlocks
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.util.placeableBlocks
import no.elg.infiniteBootleg.util.withColor
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent.Companion.selectedInventoryItemComponent
import no.elg.infiniteBootleg.world.ecs.selectedMaterialComponentFamily
import no.elg.infiniteBootleg.world.world.World
import java.lang.Math.floorMod
import kotlin.math.roundToInt

class HoveringBlockRenderer(private val worldRender: ClientWorldRender) : Renderer {

  override fun render() {
    if (ClientMain.inst().shouldIgnoreWorldInput()) {
      return
    }
    val mouseLocator = ClientMain.inst().mouseLocator
    val world = worldRender.world

    for (entity in world.engine.getEntitiesFor(selectedMaterialComponentFamily)) {
      val controls = entity.locallyControlledComponentOrNull ?: continue
      val isBreaking = controls.isBreaking
      val progress = if (isBreaking) controls.breakingProgress.progress else 1f

      val breakableBlocks = entity.breakableBlocks(world, mouseLocator.mouseBlockX, mouseLocator.mouseBlockY, controls.brushSize, controls.interactRadius)
      breakableBlocks
        .forEach { (blockWorldX, blockWorldY) ->
          if (isBreaking) {
            val textures = KAssets.breakingBlockTexture.size - 1f
            val index = (textures * progress).roundToInt()
            renderPlaceableBlock(world, KAssets.breakingBlockTexture[index].textureRegion, blockWorldX, blockWorldY, 1f)
          } else {
            renderPlaceableBlock(world, KAssets.breakableBlockTexture.textureRegion, blockWorldX, blockWorldY)
          }
        }

      if (!isBreaking) {
        val texture = entity.selectedInventoryItemComponent.material.textureRegion?.textureRegion ?: continue
        entity.placeableBlocks(world, mouseLocator.mouseBlockX, mouseLocator.mouseBlockY, controls.brushSize, controls.interactRadius)
          .forEach { (blockWorldX, blockWorldY) ->
            renderPlaceableBlock(world, texture, blockWorldX, blockWorldY)
          }
      }
    }
  }

  private fun renderPlaceableBlock(world: World, texture: TextureRegion, blockWorldX: WorldCoord, blockWorldY: WorldCoord, overrideAlpha: Float? = null) {
    val averageBrightness = world.getBlockLight(blockWorldX, blockWorldY)?.averageBrightness ?: 1f
    if (averageBrightness == 0f) {
      // no need to render a black block
      return
    }
    val a = overrideAlpha ?: (1f - averageBrightness).coerceAtLeast(0.33f)
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
