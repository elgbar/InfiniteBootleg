package no.elg.infiniteBootleg.client.util

import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.rotatableTextureName
import no.elg.infiniteBootleg.core.util.textureName
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion

fun Enum<*>.findTextures(customTextureName: String? = null): RotatableTextureRegion {
  return findTexturesOrNull(customTextureName) ?: error(
    "Could not find texture for $this with texture name '${textureName(customTextureName)}' or '${
      rotatableTextureName(
        textureName(customTextureName)
      )
    }'"
  )
}

fun Enum<*>.findTexturesOrNull(customTextureName: String? = null): RotatableTextureRegion? {
  return if (Main.Companion.isServer) {
    serverRotatableTextureRegion(customTextureName)
  } else {
    val textureName = textureName(customTextureName)
    ClientMain.inst().assets.findTextureOrNull(rotatableTextureName(textureName), rotationAllowed = true)
      ?: ClientMain.inst().assets.findTextureOrNull(textureName, rotationAllowed = false)
      ?: serverRotatableTextureRegion(customTextureName)
  }
}

fun Enum<*>.serverRotatableTextureRegion(customTextureName: String?) = RotatableTextureRegion(null, false, textureName(customTextureName))
