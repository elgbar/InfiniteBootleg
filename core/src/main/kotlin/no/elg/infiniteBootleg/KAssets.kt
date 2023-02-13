package no.elg.infiniteBootleg

import com.badlogic.gdx.Gdx
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
import java.io.File

/**
 * @author Elg
 */
object KAssets {

  private val TEXTURES_FOLDER = "textures" + File.separatorChar
  private val FONTS_FOLDER = "fonts" + File.separatorChar
  private val TEXTURES_BLOCK_FILE = TEXTURES_FOLDER + "blocks.atlas"
  private val TEXTURES_ENTITY_FILE = TEXTURES_FOLDER + "entities.atlas"

  lateinit var blockAtlas: TextureAtlas
  lateinit var entityAtlas: TextureAtlas

  private const val PLAYER_REGION_NAME = "player"
  private const val DOOR_OPEN_REGION_NAME = "door_open"
  private const val DOOR_CLOSED_REGION_NAME = "door_closed"

  lateinit var playerTexture: TextureRegion
  lateinit var doorOpenTexture: TextureRegion
  lateinit var doorClosedTexture: TextureRegion

  val font: BitmapFont by lazy {
    val generator = FreeTypeFontGenerator(Gdx.files.internal(FONTS_FOLDER + "UbuntuMono-R.ttf"))
    val parameter = FreeTypeFontParameter()
    parameter.size = ScreenRenderer.FONT_SIZE * SCALE

    parameter.minFilter = Linear
    generator.generateFont(parameter)
  }

  fun load() {
    blockAtlas = TextureAtlas(TEXTURES_BLOCK_FILE)
    entityAtlas = TextureAtlas(TEXTURES_ENTITY_FILE)

    playerTexture = TextureRegion(entityAtlas.findRegion(PLAYER_REGION_NAME))
    doorOpenTexture = TextureRegion(entityAtlas.findRegion(DOOR_OPEN_REGION_NAME))
    doorClosedTexture = TextureRegion(entityAtlas.findRegion(DOOR_CLOSED_REGION_NAME))

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
    }
  }
}
