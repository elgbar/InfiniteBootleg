package no.elg.infiniteBootleg.world.render.texture

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion

data class RotatableTextureRegion(val textureRegion: TextureRegion, val rotationAllowed: Boolean, val name: String) {

  companion object {
    fun TextureRegion.allowedRotation(name: String) = RotatableTextureRegion(this, true, name)
    fun TextureRegion.disallowedRotation(name: String) = RotatableTextureRegion(this, false, name)

    fun TextureAtlas.findRotationAwareRegionOrNull(name: String, rotationAllowed: Boolean): RotatableTextureRegion? =
      this.findRegion(name)?.let { RotatableTextureRegion(it, rotationAllowed, name) }

    fun TextureAtlas.findRotationAwareRegion(name: String, rotationAllowed: Boolean): RotatableTextureRegion =
      this.findRotationAwareRegionOrNull(name, rotationAllowed) ?: error { "Could not find region $name in texture atlas" }
  }
}
