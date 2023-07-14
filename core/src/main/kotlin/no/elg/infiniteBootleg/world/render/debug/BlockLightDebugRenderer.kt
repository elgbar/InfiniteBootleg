package no.elg.infiniteBootleg.world.render.debug

import com.badlogic.gdx.graphics.g2d.Batch
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.KAssets.luminanceDebugTexture
import no.elg.infiniteBootleg.KAssets.skylightDebugTexture
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.util.worldToScreen
import no.elg.infiniteBootleg.world.blocks.Block.Companion.BLOCK_SIZE
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.render.ClientWorldRender
import no.elg.infiniteBootleg.world.world.ClientWorld

class BlockLightDebugRenderer(private val worldRender: ClientWorldRender) : Renderer {

  override fun render() {
    val batch: Batch = worldRender.batch
    val world: ClientWorld = worldRender.world
    val worldOffsetX = world.worldBody.worldOffsetX
    val worldOffsetY = world.worldBody.worldOffsetY
    val clientMain = Main.inst() as ClientMain
    val pointerWorldX = clientMain.mouseLocator.mouseBlockX
    val pointerWorldY = clientMain.mouseLocator.mouseBlockY

    val light = world.getBlockLight(pointerWorldX, pointerWorldY, false) ?: return

    batch.begin()
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
    batch.end()
  }
}
