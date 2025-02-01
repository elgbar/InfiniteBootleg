package no.elg.infiniteBootleg.core.assets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.useDispose
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion.Companion.disallowedRotation

interface InfAssets {

  val font20pt: BitmapFont
  val font16pt: BitmapFont
  val font10pt: BitmapFont

  val breakableBlockTexture: RotatableTextureRegion
  val handTexture: RotatableTextureRegion
  val playerTexture: RotatableTextureRegion
  val doorOpenTexture: RotatableTextureRegion
  val doorClosedTexture: RotatableTextureRegion
  val pickaxeTexture: RotatableTextureRegion
  val staffTexture: RotatableTextureRegion
  val spellTexture: RotatableTextureRegion

  val skyTexture: RotatableTextureRegion
  val caveTexture: RotatableTextureRegion
  val whiteTexture: RotatableTextureRegion
  val skylightDebugTexture: RotatableTextureRegion
  val luminanceDebugTexture: RotatableTextureRegion
  val visibleAirTexture: RotatableTextureRegion

  val breakingBlockTextures: Array<RotatableTextureRegion>
  val playerIdleTextures: Animation<RotatableTextureRegion>
  val playerWalkingTextures: Animation<RotatableTextureRegion>

  /**
   * Load all assets
   */
  fun loadAssets()

  fun findTextureOrNull(name: String, rotationAllowed: Boolean): RotatableTextureRegion?

  fun findTexture(name: String, rotationAllowed: Boolean): RotatableTextureRegion

  companion object {
    const val FONTS_FOLDER = "fonts/"
    const val TEXTURES_BLOCK_FILE = "textures/textures.atlas"

    const val BREAKING_BLOCK_TEXTURE = "breaking_block"
    const val HAND_TEXTURE = "hand"
    const val PLAYER_TEXTURE = "player"
    const val DOOR_OPEN_TEXTURE = "door_open"
    const val DOOR_CLOSED_TEXTURE = "door_closed"
    const val VISIBLE_AIR_TEXTURE = "visible_air"
    const val PICKAXE_TEXTURE = "pickaxe"
    const val STICK_TEXTURE = "stick"
    const val SPELL_TEXTURE = "spell"

    const val BREAK_TEXTURE_PREFIX = "break"

    const val PLAYER_IDLE_PREFIX = "player_idle"
    const val PLAYER_WALKING_PREFIX = "player_walking"

    fun createTextureRegion(color: Color): RotatableTextureRegion = createTextureRegion(color.r, color.g, color.b, color.a)
    fun createTextureRegion(color: Color, a: Float): RotatableTextureRegion = createTextureRegion(color.r, color.g, color.b, a)
    fun createTextureRegion(r: Float, g: Float, b: Float, a: Float): RotatableTextureRegion {
      val name = "GENERATED FROM $r, $g, $b, $a"
      return if (Main.Companion.isServer) {
        RotatableTextureRegion(null, false, name)
      } else {
        Pixmap(Block.Companion.BLOCK_TEXTURE_SIZE, Block.Companion.BLOCK_TEXTURE_SIZE, Pixmap.Format.RGBA4444).useDispose {
          it.setColor(r, g, b, a)
          it.fill()
          TextureRegion(Texture(it)).disallowedRotation(name)
        }
      }
    }
  }
}
