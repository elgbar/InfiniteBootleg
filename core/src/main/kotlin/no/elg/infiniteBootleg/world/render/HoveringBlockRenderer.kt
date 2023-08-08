package no.elg.infiniteBootleg.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Interpolation
import no.elg.infiniteBootleg.KAssets
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.util.ProgressHandler
import no.elg.infiniteBootleg.util.breakableBlocks
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.util.placeableBlocks
import no.elg.infiniteBootleg.util.withColor
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent.Companion.selectedInventoryItemComponent
import no.elg.infiniteBootleg.world.ecs.components.additional.LocallyControlledComponent.Companion.locallyControlledComponentOrNull
import no.elg.infiniteBootleg.world.ecs.selectedMaterialComponentFamily
import no.elg.infiniteBootleg.world.world.World
import java.lang.Math.floorMod

class HoveringBlockRenderer(private val worldRender: ClientWorldRender) : Renderer {

  private val visualizeUpdate = ProgressHandler(1f, Interpolation.linear, 0f, 1f)
  private var target: Long? = null

  override fun render() {
    if (ClientMain.inst().shouldIgnoreWorldInput()) {
      return
    }
    val mouseLocator = ClientMain.inst().mouseLocator
    val world = worldRender.world

    if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) || target != mouseLocator.mouseBlockCompactLoc) {
      target = mouseLocator.mouseBlockCompactLoc
      visualizeUpdate.reset()
    }

    for (entity in world.engine.getEntitiesFor(selectedMaterialComponentFamily)) {
      val keyboardControls = entity.locallyControlledComponentOrNull ?: continue
      val isBreaking = !keyboardControls.instantBreak && Gdx.input.isButtonPressed(Input.Buttons.LEFT)

      val progress = if (isBreaking) {
        visualizeUpdate.calculateProgress(Gdx.graphics.deltaTime)
      } else {
        0f
      }

      val breakableBlocks = entity.breakableBlocks(world, mouseLocator.mouseBlockX, mouseLocator.mouseBlockY, keyboardControls.brushSize, keyboardControls.interactRadius)
      breakableBlocks
        .forEach { (blockWorldX, blockWorldY) ->
          if (isBreaking) {
            val textures = KAssets.breakingBlockTexture.size
            val index = (progress * textures).toInt().coerceIn(0, textures - 1)
            renderPlaceableBlock(world, KAssets.breakingBlockTexture[index].textureRegion, blockWorldX, blockWorldY)
            if (visualizeUpdate.isDone()) {
              visualizeUpdate.reset()
              world.postBox2dRunnable {
                world.removeBlocks(world.getBlocks(breakableBlocks.toList(), loadChunk = false), prioritize = true)
              }
            }
          } else {
            renderPlaceableBlock(world, KAssets.breakableBlockTexture.textureRegion, blockWorldX, blockWorldY)
          }
        }

      if (!isBreaking) {
        val texture = entity.selectedInventoryItemComponent.material.textureRegion?.textureRegion ?: continue
        entity.placeableBlocks(world, mouseLocator.mouseBlockX, mouseLocator.mouseBlockY, keyboardControls.brushSize, keyboardControls.interactRadius)
          .forEach { (blockWorldX, blockWorldY) ->
            renderPlaceableBlock(world, texture, blockWorldX, blockWorldY)
          }
      }
    }
  }

  private fun renderPlaceableBlock(world: World, texture: TextureRegion, blockWorldX: Int, blockWorldY: Int) {
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
