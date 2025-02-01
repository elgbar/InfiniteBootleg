package no.elg.infiniteBootleg.world

import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.util.findTextures
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion

fun findTexture(material: Material): RotatableTextureRegion? {
  return if (material.invisibleBlock) {
    material.findTextures(material.textureName)
  } else {
    null
  }
}

val ContainerElement.textureRegion: RotatableTextureRegion?
  get() {
    return when (this) {
      is Material -> findTexture(this)
      is Staff -> ClientMain.inst().assets.staffTexture
      is Tool -> findTextures(textureName)
    }
  }
