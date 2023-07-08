package no.elg.infiniteBootleg.world.render

import com.badlogic.gdx.graphics.g2d.TextureRegion

data class RotatableTextureRegion(val textureRegion: TextureRegion, val rotationAllowed: Boolean) {

  companion object {
    fun TextureRegion.allowedRotation() = RotatableTextureRegion(this, true)
    fun TextureRegion.disallowedRotation() = RotatableTextureRegion(this, false)
  }
}
