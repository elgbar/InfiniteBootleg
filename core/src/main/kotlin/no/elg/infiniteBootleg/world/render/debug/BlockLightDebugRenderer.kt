package no.elg.infiniteBootleg.world.render.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.LongMap
import ktx.graphics.use
import no.elg.infiniteBootleg.KAssets.luminanceDebugTexture
import no.elg.infiniteBootleg.KAssets.skylightDebugTexture
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.events.BlockLightChangedEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.util.LongMapUtil.component1
import no.elg.infiniteBootleg.util.LongMapUtil.component2
import no.elg.infiniteBootleg.util.compactChunkToWorld
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.util.worldToScreen
import no.elg.infiniteBootleg.world.blocks.Block.Companion.BLOCK_SIZE
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.render.ClientWorldRender
import no.elg.infiniteBootleg.world.render.VisualizeUpdate
import no.elg.infiniteBootleg.world.world.ClientWorld

class BlockLightDebugRenderer(private val worldRender: ClientWorldRender) : Renderer, Disposable {

  private val lr: ShapeRenderer = ShapeRenderer(1000).also {
    it.color = BLOCK_LIGHT_UPDATE_COLOR
  }

  private val newlyUpdatedChunks = LongMap<VisualizeUpdate>()
  private val listener = EventManager.registerListener { e: BlockLightChangedEvent ->
    newlyUpdatedChunks.put(compactChunkToWorld(e.chunk, e.localX, e.localY), VisualizeUpdate())
  }

  override fun render() {
    if (Settings.debugBlockLight) {
      renderLightSrc()
    }
    if (Settings.renderBlockLightUpdates) {
      renderLightUpdates()
    }
  }

  private fun renderLightUpdates() {
    val offset = BLOCK_SIZE.toFloat()
    newlyUpdatedChunks.removeAll { (_, it) -> it == null || it.isDone() }
    lr.use(ShapeRenderer.ShapeType.Filled, worldRender.camera.combined) {
      for ((compactLoc, visualizeUpdate: VisualizeUpdate?) in newlyUpdatedChunks.entries()) {
        val (worldX, worldY) = compactLoc
        lr.color.a = visualizeUpdate?.calculateAlpha(Gdx.graphics.deltaTime) ?: continue
        lr.rect(worldX * offset + 0.5f, worldY * offset + 0.5f, offset / 4f, offset / 4f)
      }
    }
  }

  private fun renderLightSrc() {
    val batch: Batch = worldRender.batch
    val world: ClientWorld = worldRender.world
    val worldOffsetX = world.worldBody.worldOffsetX
    val worldOffsetY = world.worldBody.worldOffsetY
    val mouseLocator = ClientMain.inst().mouseLocator
    val pointerWorldX = mouseLocator.mouseBlockX
    val pointerWorldY = mouseLocator.mouseBlockY

    val light = world.getBlockLight(pointerWorldX, pointerWorldY, false) ?: return
    batch.use {
      for (luminescentBlock in light.findLuminescentBlocks(pointerWorldX, pointerWorldY)) {
        val lightX = worldToScreen(luminescentBlock.worldX.toFloat(), worldOffsetX)
        val lightY = worldToScreen(luminescentBlock.worldY.toFloat(), worldOffsetY)
        batch.draw(luminanceDebugTexture.textureRegion, lightX, lightY, BLOCK_SIZE.toFloat(), BLOCK_SIZE.toFloat())
      }

      for (skyblock in light.findSkylightBlocks(pointerWorldX, pointerWorldY)) {
        val lightX = worldToScreen(skyblock.worldX.toFloat(), worldOffsetX)
        val lightY = worldToScreen(skyblock.worldY.toFloat(), worldOffsetY)
        batch.draw(skylightDebugTexture.textureRegion, lightX, lightY, BLOCK_SIZE.toFloat(), BLOCK_SIZE.toFloat())
      }
    }
  }

  override fun dispose() {
    lr.dispose()
    EventManager.removeListener(listener)
  }

  companion object {
    val BLOCK_LIGHT_UPDATE_COLOR: Color = Color.PURPLE
  }
}
