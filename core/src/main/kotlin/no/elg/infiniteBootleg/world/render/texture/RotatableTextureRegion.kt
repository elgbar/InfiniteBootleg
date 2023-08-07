package no.elg.infiniteBootleg.world.render.texture

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion

data class RotatableTextureRegion(val textureRegion: TextureRegion, val rotationAllowed: Boolean, val name: String) {

  companion object {
    fun TextureRegion.allowedRotation(name: String) = RotatableTextureRegion(this, true, name)
    fun TextureRegion.disallowedRotation(name: String) = RotatableTextureRegion(this, false, name)

    fun TextureAtlas.findRotationAwareRegionOrNull(name: String, rotationAllowed: Boolean, index: Int? = null): RotatableTextureRegion? {
      val region = index?.let { findRegion(name, it) } ?: findRegion(name)
      return region?.let { texture -> RotatableTextureRegion(texture, rotationAllowed, name) }
    }

    fun TextureAtlas.findRotationAwareRegion(name: String, rotationAllowed: Boolean, index: Int? = null): RotatableTextureRegion =
      this.findRotationAwareRegionOrNull(name, rotationAllowed, index) ?: error("Could not find region $name in texture atlas")
  }
}
