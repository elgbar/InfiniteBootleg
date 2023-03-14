package no.elg.infiniteBootleg.world.render.debug

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.util.worldToScreen
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.BlockImpl
import no.elg.infiniteBootleg.world.ClientWorld
import no.elg.infiniteBootleg.world.render.ClientWorldRender

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
      batch.draw(luminanceDebugTexture, lightX, lightY, Block.BLOCK_SIZE.toFloat(), Block.BLOCK_SIZE.toFloat())
    }

    for (skyblock in light.findSkylightBlocks(pointerWorldX, pointerWorldY)) {
      val lightX = worldToScreen(skyblock.worldX.toFloat(), worldOffsetX)
      val lightY = worldToScreen(skyblock.worldY.toFloat(), worldOffsetY)
      batch.draw(skylightDebugTexture, lightX, lightY, Block.BLOCK_SIZE.toFloat(), Block.BLOCK_SIZE.toFloat())
    }
    batch.end()
  }

  companion object {
    val skylightDebugTexture: TextureRegion by lazy {
      val pixmap = Pixmap(BlockImpl.BLOCK_SIZE, BlockImpl.BLOCK_SIZE, Pixmap.Format.RGBA4444)
      val yellow = Color.YELLOW
      pixmap.setColor(Color(yellow.r, yellow.g, yellow.b, 0.5f))
      pixmap.fill()
      val texture = Texture(pixmap)
      pixmap.dispose()
      TextureRegion(texture)
    }

    val luminanceDebugTexture: TextureRegion by lazy {
      val pixmap = Pixmap(BlockImpl.BLOCK_SIZE, BlockImpl.BLOCK_SIZE, Pixmap.Format.RGBA4444)
      val fb = Color.FIREBRICK
      pixmap.setColor(Color(fb.r, fb.g, fb.b, 0.5f))
      pixmap.fill()
      val texture = Texture(pixmap)
      pixmap.dispose()
      TextureRegion(texture)
    }
  }
}
