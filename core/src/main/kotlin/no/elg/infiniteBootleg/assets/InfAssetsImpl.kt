package no.elg.infiniteBootleg.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
import com.badlogic.gdx.graphics.g2d.BitmapFont
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
import no.elg.infiniteBootleg.assets.InfAssets.Companion.FONTS_FOLDER
import no.elg.infiniteBootleg.assets.InfAssets.Companion.TEXTURES_BLOCK_FILE
import no.elg.infiniteBootleg.assets.InfAssets.Companion.createTextureRegion
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.ClientMain.Companion.SCALE
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.screens.ScreenRenderer
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.render.ChunkRenderer
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.world.render.texture.TextureNeighbor

/**
 * @author Elg
 */
class InfAssetsImpl : InfAssets {

  override lateinit var safeTextureAtlas: SafeTextureAtlas

  override lateinit var breakableBlockTexture: RotatableTextureRegion
  override lateinit var handTexture: RotatableTextureRegion
  override lateinit var playerTexture: RotatableTextureRegion
  override lateinit var doorOpenTexture: RotatableTextureRegion
  override lateinit var doorClosedTexture: RotatableTextureRegion
  override lateinit var pickaxeTexture: RotatableTextureRegion

  override lateinit var skyTexture: RotatableTextureRegion
  override lateinit var caveTexture: RotatableTextureRegion
  override lateinit var whiteTexture: RotatableTextureRegion
  override lateinit var skylightDebugTexture: RotatableTextureRegion
  override lateinit var luminanceDebugTexture: RotatableTextureRegion
  override lateinit var visibleAirTexture: RotatableTextureRegion

  override lateinit var breakingBlockTextures: Array<RotatableTextureRegion>

  override val font: BitmapFont by lazy {
    val generator = FreeTypeFontGenerator(Gdx.files.internal(FONTS_FOLDER + "UbuntuMono-R.ttf"))
    val parameter = FreeTypeFontParameter()
    parameter.size = ScreenRenderer.FONT_SIZE * SCALE

    parameter.minFilter = Linear
    generator.generateFont(parameter)
  }

  override fun loadAssets() {
    safeTextureAtlas = SafeTextureAtlas(TEXTURES_BLOCK_FILE)

    breakableBlockTexture = safeTextureAtlas.findRotationAwareRegion("breaking_block", false)
    handTexture = safeTextureAtlas.findRotationAwareRegion("hand", false)
    playerTexture = safeTextureAtlas.findRotationAwareRegion("player", false)
    doorOpenTexture = safeTextureAtlas.findRotationAwareRegion("door_open", false)
    doorClosedTexture = safeTextureAtlas.findRotationAwareRegion("door_closed", false)
    pickaxeTexture = safeTextureAtlas.findRotationAwareRegion("pickaxe", false)
    visibleAirTexture = safeTextureAtlas.findRotationAwareRegion("visible_air", false)

    breakingBlockTextures = (1..9).map {
      safeTextureAtlas.findRotationAwareRegion("break", false, it)
    }.toTypedArray()

    skyTexture = createTextureRegion(ClientMain.CLEAR_COLOR_R, ClientMain.CLEAR_COLOR_G, ClientMain.CLEAR_COLOR_B, ClientMain.CLEAR_COLOR_A)
    caveTexture = createTextureRegion(ChunkRenderer.CAVE_CLEAR_COLOR_R, ChunkRenderer.CAVE_CLEAR_COLOR_G, ChunkRenderer.CAVE_CLEAR_COLOR_B, ClientMain.CLEAR_COLOR_A)
    whiteTexture = createTextureRegion(Color.WHITE)
    skylightDebugTexture = createTextureRegion(Color.YELLOW, 0.5f)
    luminanceDebugTexture = createTextureRegion(Color.FIREBRICK, 0.5f)

    // Do some dummy work to load textures and constructors
    Main.logger().debug("Material", "Loaded ${Material.entries.size} materials")
    TextureNeighbor.generateNeighborMap(safeTextureAtlas)

    if (Main.isClient) {
      // Only load vis ui on clients
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
    }
  }
}
