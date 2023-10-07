package no.elg.infiniteBootleg.util

import no.elg.infiniteBootleg.KAssets
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion.Companion.allowedRotation
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion.Companion.disallowedRotation

fun Enum<*>.findTextures(customTextureName: String? = null): RotatableTextureRegion {
  return findTexturesOrNull(customTextureName) ?: error("Could not find texture for $this with texture name ${textureName(customTextureName)}")
}

fun Enum<*>.findTexturesOrNull(customTextureName: String? = null): RotatableTextureRegion? {
  val textureName = textureName(customTextureName)
  val rotatableTextureName = "${textureName}_rotatable"
  return KAssets.textureAtlas.findRegion(rotatableTextureName)?.allowedRotation(rotatableTextureName)
    ?: KAssets.textureAtlas.findRegion(textureName)?.disallowedRotation(textureName)
}

fun Enum<*>.textureName(customTextureName: String? = null): String = customTextureName ?: name.lowercase()
