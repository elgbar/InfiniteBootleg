package no.elg.infiniteBootleg.core.world.render.texture

import com.badlogic.gdx.graphics.g2d.TextureRegion

data class RotatableTextureRegion(val textureRegionOrNull: TextureRegion?, val rotationAllowed: Boolean, val name: String) {

  val textureRegion get() = textureRegionOrNull ?: error("Texture region $name is null")

  companion object {
    fun TextureRegion?.allowedRotation(name: String) = RotatableTextureRegion(this, true, name)
    fun TextureRegion?.disallowedRotation(name: String) = RotatableTextureRegion(this, false, name)
  }
}
