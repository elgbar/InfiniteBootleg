package no.elg.infiniteBootleg.assets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.useDispose
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion.Companion.disallowedRotation

interface InfAssets {

  val safeTextureAtlas: SafeTextureAtlas
  val font20pt: BitmapFont
  val font10pt: BitmapFont

  val breakableBlockTexture: RotatableTextureRegion
  val handTexture: RotatableTextureRegion
  val playerTexture: RotatableTextureRegion
  val doorOpenTexture: RotatableTextureRegion
  val doorClosedTexture: RotatableTextureRegion
  val pickaxeTexture: RotatableTextureRegion

  val skyTexture: RotatableTextureRegion
  val caveTexture: RotatableTextureRegion
  val whiteTexture: RotatableTextureRegion
  val skylightDebugTexture: RotatableTextureRegion
  val luminanceDebugTexture: RotatableTextureRegion
  val visibleAirTexture: RotatableTextureRegion

  val breakingBlockTextures: Array<RotatableTextureRegion>
  val playerIdleTextures: Animation<RotatableTextureRegion>

  /**
   * Load all assets
   */
  fun loadAssets()

  companion object {
    const val FONTS_FOLDER = "fonts/"
    const val TEXTURES_BLOCK_FILE = "textures/textures.atlas"

    fun createTextureRegion(color: Color): RotatableTextureRegion = createTextureRegion(color.r, color.g, color.b, color.a)
    fun createTextureRegion(color: Color, a: Float): RotatableTextureRegion = createTextureRegion(color.r, color.g, color.b, a)
    fun createTextureRegion(r: Float, g: Float, b: Float, a: Float): RotatableTextureRegion {
      val name = "GENERATED FROM $r, $g, $b, $a"
      return if (Main.isServer) {
        RotatableTextureRegion(null, false, name)
      } else {
        Pixmap(Block.BLOCK_SIZE, Block.BLOCK_SIZE, Pixmap.Format.RGBA4444).useDispose {
          it.setColor(r, g, b, a)
          it.fill()
          TextureRegion(Texture(it)).disallowedRotation(name)
        }
      }
    }
  }
}
