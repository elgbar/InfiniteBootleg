package no.elg.infiniteBootleg.client.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.collections.plusAssign
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.render.ChunkRenderer
import no.elg.infiniteBootleg.client.world.render.texture.TextureNeighbor
import no.elg.infiniteBootleg.core.assets.InfAssets
import no.elg.infiniteBootleg.core.util.rotatableTextureName
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
class InfAssetsImpl : InfAssets {

  lateinit var safeTextureAtlas: SafeTextureAtlas

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
    val generator = FreeTypeFontGenerator(Gdx.files.internal(InfAssets.FONTS_FOLDER + "UbuntuMono-R.ttf"))
    val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
    parameter.size = pts * ClientMain.scale
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
    val array = com.badlogic.gdx.utils.Array<RotatableTextureRegion>()
    for (frame in startIndex until startIndex + totalFrames) {
      array += safeTextureAtlas.findRotationAwareRegion(regionPrefix, false, frame)
    }
    return Animation(frameDuration, array, playMode)
  }

  override val font20pt: BitmapFont by lazy { createFont(20) }
  override val font16pt: BitmapFont by lazy { createFont(16) }
  override val font10pt: BitmapFont by lazy { createFont(10) }

  override fun findTextureOrNull(name: String, rotationAllowed: Boolean): RotatableTextureRegion? {
    if (safeTextureAtlas.existsRegion(name)) {
      return safeTextureAtlas.findRotationAwareRegion(name, rotationAllowed)
    }
    return null
  }

  override fun findTexture(name: String, rotationAllowed: Boolean): RotatableTextureRegion = safeTextureAtlas.findRotationAwareRegion(name, rotationAllowed)

  override fun findTexture(name: String): RotatableTextureRegion =
    if (safeTextureAtlas.existsRegion(name)) {
      safeTextureAtlas.findRotationAwareRegion(name, false)
    } else if (safeTextureAtlas.existsRegion(rotatableTextureName(name))) {
      safeTextureAtlas.findRotationAwareRegion(rotatableTextureName(name), true)
    } else {
      throw IllegalArgumentException("No texture found with name $name or ${rotatableTextureName(name)}")
    }

  override fun loadAssets() {
    safeTextureAtlas = SafeTextureAtlas(InfAssets.TEXTURES_BLOCK_FILE)

    breakableBlockTexture = safeTextureAtlas.findRotationAwareRegion(InfAssets.BREAKING_BLOCK_TEXTURE, false)
    handTexture = safeTextureAtlas.findRotationAwareRegion(InfAssets.HAND_TEXTURE, false)
    playerTexture = safeTextureAtlas.findRotationAwareRegion(InfAssets.PLAYER_TEXTURE, false)
    doorOpenTexture = safeTextureAtlas.findRotationAwareRegion(InfAssets.DOOR_OPEN_TEXTURE, false)
    doorClosedTexture = safeTextureAtlas.findRotationAwareRegion(InfAssets.DOOR_CLOSED_TEXTURE, false)
    visibleAirTexture = safeTextureAtlas.findRotationAwareRegion(InfAssets.VISIBLE_AIR_TEXTURE, false)
    pickaxeTexture = safeTextureAtlas.findRotationAwareRegion(InfAssets.PICKAXE_TEXTURE, false)
    staffTexture = safeTextureAtlas.findRotationAwareRegion(InfAssets.STICK_TEXTURE, false)
    spellTexture = safeTextureAtlas.findRotationAwareRegion(InfAssets.SPELL_TEXTURE, false)

    breakingBlockTextures = (1..9).map {
      safeTextureAtlas.findRotationAwareRegion(InfAssets.BREAK_TEXTURE_PREFIX, false, it)
    }.toTypedArray()

    playerIdleTextures = findAnimation(InfAssets.PLAYER_IDLE_PREFIX, 3, 0.35f, startIndex = 1)
    playerWalkingTextures = findAnimation(InfAssets.PLAYER_WALKING_PREFIX, 2, 0.2f, startIndex = 0)

    skyTexture = InfAssets.createTextureRegion(
      ClientMain.CLEAR_COLOR_R,
      ClientMain.CLEAR_COLOR_G,
      ClientMain.CLEAR_COLOR_B,
      ClientMain.CLEAR_COLOR_A
    )
    caveTexture = InfAssets.createTextureRegion(
      ChunkRenderer.CAVE_CLEAR_COLOR_R,
      ChunkRenderer.CAVE_CLEAR_COLOR_G,
      ChunkRenderer.CAVE_CLEAR_COLOR_B,
      ClientMain.CLEAR_COLOR_A
    )
    whiteTexture = InfAssets.createTextureRegion(Color.WHITE)
    skylightDebugTexture = InfAssets.createTextureRegion(Color.YELLOW, 0.5f)
    luminanceDebugTexture = InfAssets.createTextureRegion(Color.FIREBRICK, 0.5f)

    // Do some dummy work to load textures and constructors
    logger.debug { "Loaded ${Material.normalMaterials.size} materials" }
    TextureNeighbor.generateNeighborMap(safeTextureAtlas.regions)

    loadInfBootSkin()
  }
}
