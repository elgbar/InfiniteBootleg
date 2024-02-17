package no.elg.infiniteBootleg.world.render

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.g2d.TextureRegion
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.items.ItemType
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.Progress
import no.elg.infiniteBootleg.util.WorldCompactLoc
import no.elg.infiniteBootleg.util.breakableLocs
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.util.placeableBlocks
import no.elg.infiniteBootleg.util.withColor
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.inventory.HotbarComponent.Companion.selectedItem
import no.elg.infiniteBootleg.world.ecs.components.transients.CurrentlyBreakingComponent.Companion.currentlyBreakingComponentOrNull
import no.elg.infiniteBootleg.world.ecs.selectedMaterialComponentFamily
import no.elg.infiniteBootleg.world.world.World
import java.lang.Math.floorMod
import kotlin.math.roundToInt

class HoveringBlockRenderer(private val worldRender: ClientWorldRender) : Renderer {

  private val entities: ImmutableArray<Entity> = worldRender.world.engine.getEntitiesFor(selectedMaterialComponentFamily)

  override fun render() {
    if (ClientMain.inst().shouldIgnoreWorldInput()) {
      return
    }
    val mouseLocator = ClientMain.inst().mouseLocator
    val world = worldRender.world

    for (entity in entities) {
      val controls = entity.locallyControlledComponentOrNull ?: continue
      val element = entity.selectedItem?.element ?: continue
      val isBreaking = controls.isBreaking(entity)
      val breakingComponent = entity.currentlyBreakingComponentOrNull

      if (element.itemType == ItemType.TOOL) {
        val breakableBlocks = entity.breakableLocs(world, mouseLocator.mouseBlockX, mouseLocator.mouseBlockY, controls.brushSize, controls.interactRadius)
        breakableBlocks.forEach { blockWorldLoc ->
          if (isBreaking) {
            val rawProgress: Progress = breakingComponent?.breaking?.get(blockWorldLoc)?.progressHandler?.progress ?: run { 0f }
            if (Main.isServerClient) {
              ClientMain.inst().serverClient?.let {
                val serverProgress = it.breakingBlockCache.getIfPresent(blockWorldLoc)
                if (serverProgress != null && serverProgress > rawProgress) {
                  // If the server progress is greater than the local progress, do not render the local progress
                  return@forEach
                }
              }
            }
            renderBreakingOverlay(world, blockWorldLoc, rawProgress)
          } else {
            renderPlaceableBlock(world, Main.inst().assets.breakableBlockTexture.textureRegion, blockWorldLoc)
          }
        }
      } else if (element.itemType == ItemType.BLOCK && !isBreaking) {
        val texture = element.textureRegion?.textureRegionOrNull ?: continue
        entity.placeableBlocks(world, mouseLocator.mouseBlockX, mouseLocator.mouseBlockY, controls.interactRadius)
          .forEach { blockWorldLoc ->
            renderPlaceableBlock(world, texture, blockWorldLoc)
          }
      }
      if (Main.isServerClient) {
        ClientMain.inst().serverClient?.let {
          it.breakingBlockCache.asMap()
            .forEach { (blockWorldLoc, rawProgress) ->
              renderBreakingOverlay(world, blockWorldLoc, rawProgress)
            }
        }
      }
    }
  }

  private fun renderBreakingOverlay(world: World, blockWorldLoc: WorldCompactLoc, rawProgress: Progress) {
    val progress = rawProgress.coerceIn(0f, 1f)
    if (progress == 0f) {
      // Render nothing when there is no progress made yet
      return
    }
    val textures = Main.inst().assets.breakingBlockTextures.size - 1f
    val index = (textures * progress).roundToInt()
    renderPlaceableBlock(world, Main.inst().assets.breakingBlockTextures[index].textureRegion, blockWorldLoc, 1f)
  }

  private fun renderPlaceableBlock(world: World, texture: TextureRegion, blockWorldLoc: WorldCompactLoc, overrideAlpha: Float? = null) {
    val (blockWorldX, blockWorldY) = blockWorldLoc
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
