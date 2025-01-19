package no.elg.infiniteBootleg.util

import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion.Companion.allowedRotation
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion.Companion.disallowedRotation
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
  if (Main.isServer) {
    return serverRotatableTextureRegion(customTextureName)
  } else {
    val textureName = textureName(customTextureName)
    val rotatableTextureName = rotatableTextureName(textureName)
    return ClientMain.inst().assets.safeTextureAtlas.findRegion(rotatableTextureName)?.allowedRotation(rotatableTextureName)
      ?: ClientMain.inst().assets.safeTextureAtlas.findRegion(textureName)?.disallowedRotation(textureName)
      ?: serverRotatableTextureRegion(customTextureName)
  }
}

fun Enum<*>.textureName(customTextureName: String? = null): String = customTextureName ?: name.lowercase()
fun rotatableTextureName(textureName: String): String = "${textureName}_rotatable"

fun Enum<*>.serverRotatableTextureRegion(customTextureName: String?) = RotatableTextureRegion(null, false, textureName(customTextureName))

inline fun <reified T> valueOfOrNull(name: String): T? where T : Enum<T> = EnumUtils.getEnumIgnoreCase(T::class.java, name)
