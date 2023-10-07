package no.elg.infiniteBootleg.assets

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.utils.Array
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion

class SafeTextureAtlas(textFileName: String) {

  private val textureAtlas: TextureAtlas?

  init {
    textureAtlas = try {
      TextureAtlas(textFileName)
    } catch (e: Throwable) {
      null
    }
  }

  val regions: Array<AtlasRegion> get() = textureAtlas?.regions ?: Array(false, 0)

  fun findRegion(name: String, index: Int? = null): AtlasRegion? {
    return index?.let { textureAtlas?.findRegion(name, it) } ?: textureAtlas?.findRegion(name)
  }

  fun findRotationAwareRegion(name: String, rotationAllowed: Boolean, index: Int? = null): RotatableTextureRegion {
    return RotatableTextureRegion(findRegion(name, index), rotationAllowed, name)
  }
}
