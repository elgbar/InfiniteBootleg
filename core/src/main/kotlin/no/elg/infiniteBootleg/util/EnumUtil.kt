package no.elg.infiniteBootleg.util

import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion
import org.apache.commons.lang3.EnumUtils

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
  return if (Main.isServer) {
    serverRotatableTextureRegion(customTextureName)
  } else {
    val textureName = textureName(customTextureName)
    ClientMain.inst().assets.findTextureOrNull(rotatableTextureName(textureName), rotationAllowed = true)
      ?: ClientMain.inst().assets.findTextureOrNull(textureName, rotationAllowed = false)
      ?: serverRotatableTextureRegion(customTextureName)
  }
}

fun Enum<*>.textureName(customTextureName: String? = null): String = customTextureName ?: name.lowercase()
fun rotatableTextureName(textureName: String): String = "${textureName}_rotatable"

fun Enum<*>.serverRotatableTextureRegion(customTextureName: String?) = RotatableTextureRegion(null, false, textureName(customTextureName))

inline fun <reified T> valueOfOrNull(name: String): T? where T : Enum<T> = EnumUtils.getEnumIgnoreCase(T::class.java, name)
