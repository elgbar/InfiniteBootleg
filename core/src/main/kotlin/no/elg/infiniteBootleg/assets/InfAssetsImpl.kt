package no.elg.infiniteBootleg.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.collections.GdxArray
import ktx.collections.plusAssign
import no.elg.infiniteBootleg.assets.InfAssets.Companion.BREAKING_BLOCK_TEXTURE
import no.elg.infiniteBootleg.assets.InfAssets.Companion.BREAK_TEXTURE_PREFIX
import no.elg.infiniteBootleg.assets.InfAssets.Companion.DOOR_CLOSED_TEXTURE
import no.elg.infiniteBootleg.assets.InfAssets.Companion.DOOR_OPEN_TEXTURE
import no.elg.infiniteBootleg.assets.InfAssets.Companion.FONTS_FOLDER
import no.elg.infiniteBootleg.assets.InfAssets.Companion.HAND_TEXTURE
import no.elg.infiniteBootleg.assets.InfAssets.Companion.PICKAXE_TEXTURE
import no.elg.infiniteBootleg.assets.InfAssets.Companion.PLAYER_IDLE_PREFIX
import no.elg.infiniteBootleg.assets.InfAssets.Companion.PLAYER_TEXTURE
import no.elg.infiniteBootleg.assets.InfAssets.Companion.PLAYER_WALKING_PREFIX
import no.elg.infiniteBootleg.assets.InfAssets.Companion.SPELL_TEXTURE
import no.elg.infiniteBootleg.assets.InfAssets.Companion.STICK_TEXTURE
import no.elg.infiniteBootleg.assets.InfAssets.Companion.TEXTURES_BLOCK_FILE
import no.elg.infiniteBootleg.assets.InfAssets.Companion.VISIBLE_AIR_TEXTURE
import no.elg.infiniteBootleg.assets.InfAssets.Companion.createTextureRegion
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.ClientMain.Companion.scale
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.render.ChunkRenderer
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.world.render.texture.TextureNeighbor

private val logger = KotlinLogging.logger {}

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
  override lateinit var staffTexture: RotatableTextureRegion
  override lateinit var spellTexture: RotatableTextureRegion

  override lateinit var skyTexture: RotatableTextureRegion
  override lateinit var caveTexture: RotatableTextureRegion
  override lateinit var whiteTexture: RotatableTextureRegion
  override lateinit var skylightDebugTexture: RotatableTextureRegion
  override lateinit var luminanceDebugTexture: RotatableTextureRegion
  override lateinit var visibleAirTexture: RotatableTextureRegion

  override lateinit var breakingBlockTextures: Array<RotatableTextureRegion>
  override lateinit var playerIdleTextures: Animation<RotatableTextureRegion>
  override lateinit var playerWalkingTextures: Animation<RotatableTextureRegion>

  private fun createFont(pts: Int): BitmapFont {
    val generator = FreeTypeFontGenerator(Gdx.files.internal(FONTS_FOLDER + "UbuntuMono-R.ttf"))
    val parameter = FreeTypeFontParameter()
    parameter.size = pts * scale
    parameter.minFilter = Texture.TextureFilter.Linear
    parameter.magFilter = Texture.TextureFilter.MipMapLinearLinear
    parameter.genMipMaps = true
    return generator.generateFont(parameter).also { it.setUseIntegerPositions(false) }
  }

  private fun findAnimation(
    regionPrefix: String,
    totalFrames: Int,
    frameDuration: Float,
    startIndex: Int = 0,
    playMode: Animation.PlayMode = Animation.PlayMode.LOOP_PINGPONG
  ): Animation<RotatableTextureRegion> {
    val array = GdxArray<RotatableTextureRegion>()
    for (frame in startIndex until startIndex + totalFrames) {
      array += safeTextureAtlas.findRotationAwareRegion(regionPrefix, false, frame)
    }
    return Animation(frameDuration, array, playMode)
  }

  override val font20pt: BitmapFont by lazy { createFont(20) }
  override val font16pt: BitmapFont by lazy { createFont(16) }
  override val font10pt: BitmapFont by lazy { createFont(10) }

  override fun loadAssets() {
    safeTextureAtlas = SafeTextureAtlas(TEXTURES_BLOCK_FILE)

    breakableBlockTexture = safeTextureAtlas.findRotationAwareRegion(BREAKING_BLOCK_TEXTURE, false)
    handTexture = safeTextureAtlas.findRotationAwareRegion(HAND_TEXTURE, false)
    playerTexture = safeTextureAtlas.findRotationAwareRegion(PLAYER_TEXTURE, false)
    doorOpenTexture = safeTextureAtlas.findRotationAwareRegion(DOOR_OPEN_TEXTURE, false)
    doorClosedTexture = safeTextureAtlas.findRotationAwareRegion(DOOR_CLOSED_TEXTURE, false)
    visibleAirTexture = safeTextureAtlas.findRotationAwareRegion(VISIBLE_AIR_TEXTURE, false)
    pickaxeTexture = safeTextureAtlas.findRotationAwareRegion(PICKAXE_TEXTURE, false)
    staffTexture = safeTextureAtlas.findRotationAwareRegion(STICK_TEXTURE, false)
    spellTexture = safeTextureAtlas.findRotationAwareRegion(SPELL_TEXTURE, false)

    breakingBlockTextures = (1..9).map {
      safeTextureAtlas.findRotationAwareRegion(BREAK_TEXTURE_PREFIX, false, it)
    }.toTypedArray()

    playerIdleTextures = findAnimation(PLAYER_IDLE_PREFIX, 3, 0.35f, startIndex = 1)
    playerWalkingTextures = findAnimation(PLAYER_WALKING_PREFIX, 2, 0.2f, startIndex = 0)

    skyTexture = createTextureRegion(ClientMain.CLEAR_COLOR_R, ClientMain.CLEAR_COLOR_G, ClientMain.CLEAR_COLOR_B, ClientMain.CLEAR_COLOR_A)
    caveTexture = createTextureRegion(ChunkRenderer.CAVE_CLEAR_COLOR_R, ChunkRenderer.CAVE_CLEAR_COLOR_G, ChunkRenderer.CAVE_CLEAR_COLOR_B, ClientMain.CLEAR_COLOR_A)
    whiteTexture = createTextureRegion(Color.WHITE)
    skylightDebugTexture = createTextureRegion(Color.YELLOW, 0.5f)
    luminanceDebugTexture = createTextureRegion(Color.FIREBRICK, 0.5f)

    // Do some dummy work to load textures and constructors
    logger.debug { "Loaded ${Material.normalMaterials.size} materials" }
    TextureNeighbor.generateNeighborMap(safeTextureAtlas)

    loadInfBootSkin()
  }
}
