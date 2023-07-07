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
import no.elg.infiniteBootleg.ClientMain.SCALE
import no.elg.infiniteBootleg.screen.ScreenRenderer
import no.elg.infiniteBootleg.util.use
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.render.ChunkRenderer
import java.io.File

/**
 * @author Elg
 */
object KAssets {

  private val TEXTURES_FOLDER = "textures" + File.separatorChar
  private val FONTS_FOLDER = "fonts" + File.separatorChar
  private val TEXTURES_BLOCK_FILE = "${TEXTURES_FOLDER}textures.atlas"

  lateinit var textureAtlas: TextureAtlas

  private const val HAND_REGION_NAME = "hand"
  private const val PLAYER_REGION_NAME = "player"
  private const val DOOR_OPEN_REGION_NAME = "door_open"
  private const val DOOR_CLOSED_REGION_NAME = "door_closed"

  lateinit var handTexture: TextureRegion
  lateinit var playerTexture: TextureRegion
  lateinit var doorOpenTexture: TextureRegion
  lateinit var doorClosedTexture: TextureRegion

  lateinit var skyTexture: TextureRegion
  lateinit var caveTexture: TextureRegion
  lateinit var whiteTexture: TextureRegion
  lateinit var skylightDebugTexture: TextureRegion
  lateinit var luminanceDebugTexture: TextureRegion

  val font: BitmapFont by lazy {
    val generator = FreeTypeFontGenerator(Gdx.files.internal(FONTS_FOLDER + "UbuntuMono-R.ttf"))
    val parameter = FreeTypeFontParameter()
    parameter.size = ScreenRenderer.FONT_SIZE * SCALE

    parameter.minFilter = Linear
    generator.generateFont(parameter)
  }

  fun load() {
    textureAtlas = TextureAtlas(TEXTURES_BLOCK_FILE)

    handTexture = TextureRegion(textureAtlas.findRegion(HAND_REGION_NAME))
    playerTexture = TextureRegion(textureAtlas.findRegion(PLAYER_REGION_NAME))
    doorOpenTexture = TextureRegion(textureAtlas.findRegion(DOOR_OPEN_REGION_NAME))
    doorClosedTexture = TextureRegion(textureAtlas.findRegion(DOOR_CLOSED_REGION_NAME))

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
    Main.logger().debug("Material", "Loaded ${Material.values().size} materials")
  }

  private fun createTextureRegion(color: Color): TextureRegion = createTextureRegion(color.r, color.g, color.b, color.a)
  private fun createTextureRegion(color: Color, a: Float): TextureRegion = createTextureRegion(color.r, color.g, color.b, a)
  private fun createTextureRegion(r: Float, g: Float, b: Float, a: Float): TextureRegion =
    Pixmap(Block.BLOCK_SIZE, Block.BLOCK_SIZE, Pixmap.Format.RGBA4444).use<Pixmap, TextureRegion> {
      it.setColor(r, g, b, a)
      it.fill()
      return@use TextureRegion(Texture(it))
    }
}
