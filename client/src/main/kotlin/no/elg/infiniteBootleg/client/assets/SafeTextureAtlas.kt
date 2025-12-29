package no.elg.infiniteBootleg.client.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion

class SafeTextureAtlas(textFileName: String) {

  private val textureAtlas: TextureAtlas?
  private val textureAtlasData: TextureAtlas.TextureAtlasData
  private val cache = mutableMapOf<String, RotatableTextureRegion>()

  init {
    val fileHandle = Gdx.files.internal(textFileName)
    textureAtlasData = TextureAtlas.TextureAtlasData(fileHandle, fileHandle.parent(), false)

    textureAtlas = try {
      TextureAtlas(textureAtlasData)
    } catch (e: Throwable) {
      null
    }
  }

  val regions: Array<TextureAtlas.AtlasRegion> get() = textureAtlas?.regions ?: Array(false, 0)

  fun existsRegion(name: String, index: Int = -1): Boolean = textureAtlasData.regions.any { region -> region.name == name && (index == -1 || region.index == index) }

  fun findRegion(name: String, index: Int? = null): TextureAtlas.AtlasRegion? = index?.let { textureAtlas?.findRegion(name, it) } ?: textureAtlas?.findRegion(name)

  fun findRotationAwareRegion(name: String, rotationAllowed: Boolean, maybeIndex: Int? = null): RotatableTextureRegion =
    findRotationAwareRegionOrNull(name, rotationAllowed, maybeIndex) ?: throw IllegalArgumentException("Could not find region with name $name and index $maybeIndex")

  fun findRotationAwareRegionOrNull(name: String, rotationAllowed: Boolean, maybeIndex: Int? = null): RotatableTextureRegion? =
    cache.getOrPut(name + rotationAllowed + maybeIndex) {
      val textureRegion = findRegion(name, maybeIndex) ?: return null
      RotatableTextureRegion(textureRegion, rotationAllowed, name)
    }
}
