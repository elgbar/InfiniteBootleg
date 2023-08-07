package no.elg.infiniteBootleg

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.kotcrab.vis.ui.VisUI
import ktx.style.imageTextButton
import ktx.style.label
import ktx.style.menu
import ktx.style.menuItem
import ktx.style.set
import ktx.style.textButton
import ktx.style.textField
import ktx.style.visCheckBox
import ktx.style.visImageTextButton
import ktx.style.visTextButton
import ktx.style.visTextField
import ktx.style.window
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.ClientMain.Companion.SCALE
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.screens.ScreenRenderer
import no.elg.infiniteBootleg.util.useDispose
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.render.ChunkRenderer
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion.Companion.disallowedRotation
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion.Companion.findRotationAwareRegion
import no.elg.infiniteBootleg.world.render.texture.TextureNeighbor
import java.io.File

/**
 * @author Elg
 */
object KAssets {

  private val TEXTURES_FOLDER = "textures" + File.separatorChar
  private val FONTS_FOLDER = "fonts" + File.separatorChar
  private val TEXTURES_BLOCK_FILE = "${TEXTURES_FOLDER}textures.atlas"

  lateinit var textureAtlas: TextureAtlas

  lateinit var breakableBlockTexture: RotatableTextureRegion
  lateinit var handTexture: RotatableTextureRegion
  lateinit var playerTexture: RotatableTextureRegion
  lateinit var doorOpenTexture: RotatableTextureRegion
  lateinit var doorClosedTexture: RotatableTextureRegion

  lateinit var skyTexture: RotatableTextureRegion
  lateinit var caveTexture: RotatableTextureRegion
  lateinit var whiteTexture: RotatableTextureRegion
  lateinit var skylightDebugTexture: RotatableTextureRegion
  lateinit var luminanceDebugTexture: RotatableTextureRegion

  lateinit var breakingBlockTexture: Array<RotatableTextureRegion>

  val font: BitmapFont by lazy {
    val generator = FreeTypeFontGenerator(Gdx.files.internal(FONTS_FOLDER + "UbuntuMono-R.ttf"))
    val parameter = FreeTypeFontParameter()
    parameter.size = ScreenRenderer.FONT_SIZE * SCALE

    parameter.minFilter = Linear
    generator.generateFont(parameter)
  }

  fun loadAssets() {
    textureAtlas = TextureAtlas(TEXTURES_BLOCK_FILE)

    breakableBlockTexture = textureAtlas.findRotationAwareRegion("breaking_block", false)
    handTexture = textureAtlas.findRotationAwareRegion("hand", false)
    playerTexture = textureAtlas.findRotationAwareRegion("player", false)
    doorOpenTexture = textureAtlas.findRotationAwareRegion("door_open", false)
    doorClosedTexture = textureAtlas.findRotationAwareRegion("door_closed", false)

    breakingBlockTexture = (1..9).map {
      textureAtlas.findRotationAwareRegion("break", false, it)
    }.toTypedArray()

    with(VisUI.getSkin() as Skin) {
      val notFlippedFont = font
      val boldNotFlippedFont = font

      this["default-font"] = notFlippedFont

      label(extend = "default") { font = notFlippedFont }
      label(extend = "link-label") { font = notFlippedFont }
      label(extend = "small") { font = notFlippedFont }
      label(extend = "menuitem-shortcut") { font = notFlippedFont }

      visTextField(extend = "default") { font = notFlippedFont }
      textField(extend = "default") { font = notFlippedFont }

      visTextButton(extend = "default") { font = notFlippedFont }
      visTextButton(extend = "menu-bar") { font = notFlippedFont }
      visTextButton(extend = "toggle") { font = notFlippedFont }
      visTextButton(extend = "blue") { font = notFlippedFont }

      visCheckBox(extend = "default") { font = notFlippedFont }

      textButton(extend = "default") { font = notFlippedFont }

      val newOpenButtonStyle = visImageTextButton(extend = "default") { font = notFlippedFont }
      visImageTextButton(extend = "menu-bar") { font = notFlippedFont }
      imageTextButton(extend = "default") { font = notFlippedFont }

      window(extend = "default") { titleFont = boldNotFlippedFont }
      window(extend = "resizable") { titleFont = boldNotFlippedFont }
      window(extend = "noborder") { titleFont = boldNotFlippedFont }
      window(extend = "dialog") { titleFont = boldNotFlippedFont }

      menuItem(extend = "default") { font = notFlippedFont }
      menu { openButtonStyle = newOpenButtonStyle }

      visTextButton(name = "debug-menu-button", extend = "default") {
        font = notFlippedFont
        fontColor = Color.WHITE
        disabledFontColor = Color.WHITE
        down = newDrawable("white", Color.valueOf("#FF4136"))
        up = down
        over = newDrawable("white", Color.FIREBRICK)
        checkedOver = over
        disabled = newDrawable("white", Color.FOREST)
      }
    }

    skyTexture = createTextureRegion(ClientMain.CLEAR_COLOR_R, ClientMain.CLEAR_COLOR_G, ClientMain.CLEAR_COLOR_B, ClientMain.CLEAR_COLOR_A)
    caveTexture = createTextureRegion(ChunkRenderer.CAVE_CLEAR_COLOR_R, ChunkRenderer.CAVE_CLEAR_COLOR_G, ChunkRenderer.CAVE_CLEAR_COLOR_B, ClientMain.CLEAR_COLOR_A)
    whiteTexture = createTextureRegion(Color.WHITE)
    skylightDebugTexture = createTextureRegion(Color.YELLOW, 0.5f)
    luminanceDebugTexture = createTextureRegion(Color.FIREBRICK, 0.5f)

    // Do some dummy work to load textures and constructors
    Main.logger().debug("Material", "Loaded ${Material.entries.size} materials")

    TextureNeighbor.generateNeighborMap(textureAtlas)
  }

  private fun createTextureRegion(color: Color): RotatableTextureRegion = createTextureRegion(color.r, color.g, color.b, color.a)
  private fun createTextureRegion(color: Color, a: Float): RotatableTextureRegion = createTextureRegion(color.r, color.g, color.b, a)
  private fun createTextureRegion(r: Float, g: Float, b: Float, a: Float): RotatableTextureRegion =
    Pixmap(Block.BLOCK_SIZE, Block.BLOCK_SIZE, Pixmap.Format.RGBA4444).useDispose {
      it.setColor(r, g, b, a)
      it.fill()
      TextureRegion(Texture(it)).disallowedRotation("GENERATED FROM $r, $g, $b, $a")
    }
}
