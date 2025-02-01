package no.elg.infiniteBootleg.client.world.render

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.g2d.TextureRegion
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.textureRegion
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.api.Renderer
import no.elg.infiniteBootleg.core.items.ItemType
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.Progress
import no.elg.infiniteBootleg.core.util.WorldCompactLoc
import no.elg.infiniteBootleg.core.util.breakableLocs
import no.elg.infiniteBootleg.core.util.component1
import no.elg.infiniteBootleg.core.util.component2
import no.elg.infiniteBootleg.core.util.placeableBlocks
import no.elg.infiniteBootleg.core.util.withColor
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.BlockLight
import no.elg.infiniteBootleg.core.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.HotbarComponent.Companion.selectedItem
import no.elg.infiniteBootleg.core.world.ecs.components.transients.CurrentlyBreakingComponent.Companion.currentlyBreakingComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.selectedMaterialComponentFamily
import no.elg.infiniteBootleg.core.world.world.World
import kotlin.math.roundToInt

class HoveringBlockRenderer(private val worldRender: ClientWorldRender) : Renderer {

  private val entities: ImmutableArray<Entity> = worldRender.world.engine.getEntitiesFor(selectedMaterialComponentFamily)

  override fun render() {
    val world = worldRender.world
    if (Main.Companion.isServerClient) {
      ClientMain.inst().serverClient?.breakingBlockCache?.asMap()?.forEach { (blockWorldLoc, rawProgress) ->
        renderBreakingOverlay(world, blockWorldLoc, rawProgress)
      }
    }

    if (ClientMain.inst().shouldIgnoreWorldInput()) {
      return
    }
    val mouseLocator = ClientMain.inst().mouseLocator

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
            if (Main.Companion.isServerClient) {
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
            renderPlaceableBlock(world, ClientMain.inst().assets.breakableBlockTexture.textureRegion, blockWorldLoc)
          }
        }
      } else if (element.itemType == ItemType.BLOCK && !isBreaking) {
        val texture = element.textureRegion?.textureRegionOrNull ?: continue
        entity.placeableBlocks(world, mouseLocator.mouseBlockX, mouseLocator.mouseBlockY, controls.interactRadius)
          .forEach { blockWorldLoc ->
            renderPlaceableBlock(world, texture, blockWorldLoc)
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
    val textures = ClientMain.inst().assets.breakingBlockTextures.size - 1f
    val index = (textures * progress).roundToInt()
    renderPlaceableBlock(world, ClientMain.inst().assets.breakingBlockTextures[index].textureRegion, blockWorldLoc, 1f)
  }

  private fun renderPlaceableBlock(world: World, texture: TextureRegion, blockWorldLoc: WorldCompactLoc, overrideAlpha: Float? = null) {
    val (blockWorldX, blockWorldY) = blockWorldLoc
    val averageBrightness = if (Settings.renderLight) {
      val blockBrightness = world.getBlockLight(blockWorldX, blockWorldY)?.averageBrightness ?: BlockLight.Companion.FULL_BRIGHTNESS
      if (blockBrightness == BlockLight.Companion.COMPLETE_DARKNESS) {
        // no need to render a black block
        return
      }
      blockBrightness
    } else {
      BlockLight.Companion.FULL_BRIGHTNESS
    }
    val alpha = overrideAlpha ?: (1f - averageBrightness).coerceAtLeast(0.33f)
    worldRender.batch.withColor(averageBrightness, averageBrightness, averageBrightness, alpha) {
      val mouseScreenX = blockWorldX * Block.Companion.BLOCK_TEXTURE_SIZE
      val mouseScreenY = blockWorldY * Block.Companion.BLOCK_TEXTURE_SIZE
      val diffFromBlockSizeX = Math.floorMod(mouseScreenX, Block.Companion.BLOCK_TEXTURE_SIZE).toFloat()
      val diffFromBlockSizeY = Math.floorMod(mouseScreenY, Block.Companion.BLOCK_TEXTURE_SIZE).toFloat()
      it.draw(
        texture,
        // Draw the block aligned to the block grid
        mouseScreenX - diffFromBlockSizeX,
        mouseScreenY - diffFromBlockSizeY,
        0f,
        0f,
        Block.Companion.BLOCK_TEXTURE_SIZE.toFloat(),
        Block.Companion.BLOCK_TEXTURE_SIZE.toFloat(),
        1f,
        1f,
        0f
      )
    }
  }
}
