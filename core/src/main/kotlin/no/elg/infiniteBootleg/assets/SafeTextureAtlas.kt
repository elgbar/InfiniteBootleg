package no.elg.infiniteBootleg.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.utils.Array
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion

class SafeTextureAtlas(textFileName: String) {

  private val textureAtlas: TextureAtlas?
  private val textureAtlasData: TextureAtlas.TextureAtlasData

  init {
    val fileHandle = Gdx.files.internal(textFileName)
    textureAtlasData = TextureAtlas.TextureAtlasData(fileHandle, fileHandle.parent(), false)

    textureAtlas = try {
      TextureAtlas(textureAtlasData)
    } catch (e: Throwable) {
      null
    }
  }

  val regions: Array<AtlasRegion> get() = textureAtlas?.regions ?: Array(false, 0)

  fun existsRegion(name: String, index: Int = -1): Boolean = textureAtlasData.regions.any { region -> region.name == name && (index == -1 || region.index == index) }

  fun findRegion(name: String, index: Int? = null): AtlasRegion? {
    return index?.let { textureAtlas?.findRegion(name, it) } ?: textureAtlas?.findRegion(name)
  }

  fun findRotationAwareRegion(name: String, rotationAllowed: Boolean, index: Int? = null): RotatableTextureRegion {
    return RotatableTextureRegion(findRegion(name, index), rotationAllowed, name)
  }
}
