package no.elg.infiniteBootleg.util

import no.elg.infiniteBootleg.KAssets
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion.Companion.allowedRotation
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion.Companion.disallowedRotation

fun Enum<*>.findTextures(customTextureName: String? = null): RotatableTextureRegion {
  val textureName = customTextureName ?: name.lowercase()
  val rotatableTextureName = "${textureName}_rotatable"
  return KAssets.textureAtlas.findRegion(rotatableTextureName)?.allowedRotation(rotatableTextureName)
    ?: KAssets.textureAtlas.findRegion(textureName).disallowedRotation(textureName)
}
