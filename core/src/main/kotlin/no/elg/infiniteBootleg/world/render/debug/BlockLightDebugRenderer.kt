package no.elg.infiniteBootleg.world.render.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.LongMap
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.render.OverlayRenderer
import no.elg.infiniteBootleg.events.BlockLightChangedEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.LongMapUtil.component1
import no.elg.infiniteBootleg.util.LongMapUtil.component2
import no.elg.infiniteBootleg.util.ProgressHandler
import no.elg.infiniteBootleg.util.compactChunkToWorld
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.util.safeUse
import no.elg.infiniteBootleg.util.worldToScreen
import no.elg.infiniteBootleg.world.blocks.Block.Companion.BLOCK_SIZE
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.render.ClientWorldRender
import no.elg.infiniteBootleg.world.world.ClientWorld

class BlockLightDebugRenderer(private val worldRender: ClientWorldRender) : OverlayRenderer, Disposable {

  private val shapeRenderer: ShapeRenderer = ShapeRenderer(1000).also {
    it.color = BLOCK_LIGHT_UPDATE_COLOR
  }

  private val newlyUpdatedChunks = LongMap<ProgressHandler>()
  private val listener = EventManager.registerListener { e: BlockLightChangedEvent ->
    if (Settings.renderBlockLightUpdates) {
      newlyUpdatedChunks.put(compactChunkToWorld(e.chunk, e.localX, e.localY), ProgressHandler(1f))
    }
  }

  private val skylightDebugTexture by lazy { Main.inst().assets.skylightDebugTexture.textureRegion }
  private val luminanceDebugTexture by lazy { Main.inst().assets.luminanceDebugTexture.textureRegion }

  override val isActive: Boolean
    get() = Settings.renderLight && (Settings.debugBlockLight || Settings.renderBlockLightUpdates)

  override fun render() {
    if (Settings.debugBlockLight) {
      renderLightSrc()
    }
    if (Settings.renderBlockLightUpdates) {
      renderLightUpdates()
    }
  }

  private fun renderLightUpdates() {
    newlyUpdatedChunks.removeAll { (_, it: ProgressHandler?) -> it == null || it.isDone() }
    Gdx.gl.glEnable(GL30.GL_BLEND)
    shapeRenderer.safeUse(ShapeRenderer.ShapeType.Filled, worldRender.camera.combined) {
      for ((compactLoc, visualizeUpdate: ProgressHandler?) in newlyUpdatedChunks.entries()) {
        val (worldX, worldY) = compactLoc
        shapeRenderer.color.a = visualizeUpdate?.updateAndGetProgress(Gdx.graphics.deltaTime) ?: continue
        shapeRenderer.rect(worldX * TEXTURE_SIZE + TEXTURE_SIZE / 2f, worldY * TEXTURE_SIZE + TEXTURE_SIZE / 2f, TEXTURE_SIZE / 4f, TEXTURE_SIZE / 4f)
      }
    }
  }

  private fun renderLightSrc() {
    val batch: Batch = worldRender.batch
    val world: ClientWorld = worldRender.world
    val mouseLocator = ClientMain.inst().mouseLocator
    val pointerWorldX = mouseLocator.mouseBlockX
    val pointerWorldY = mouseLocator.mouseBlockY

    val light = world.getBlockLight(pointerWorldX, pointerWorldY, false) ?: return
    for (luminescentBlock in light.findLuminescentBlocks(pointerWorldX, pointerWorldY)) {
      val lightX = worldToScreen(luminescentBlock.worldX.toFloat())
      val lightY = worldToScreen(luminescentBlock.worldY.toFloat())
      batch.draw(luminanceDebugTexture, lightX, lightY, BLOCK_SIZE.toFloat(), BLOCK_SIZE.toFloat())
    }

    for (skyblock in light.findSkylightBlocks(pointerWorldX, pointerWorldY)) {
      val lightX = worldToScreen(skyblock.worldX.toFloat())
      val lightY = worldToScreen(skyblock.worldY.toFloat())
      batch.draw(skylightDebugTexture, lightX, lightY, BLOCK_SIZE.toFloat(), BLOCK_SIZE.toFloat())
    }
  }

  override fun dispose() {
    shapeRenderer.dispose()
    listener.removeListener()
  }

  companion object {
    val BLOCK_LIGHT_UPDATE_COLOR: Color = Color.PURPLE
    const val TEXTURE_SIZE = BLOCK_SIZE.toFloat()
  }
}
