package no.elg.infiniteBootleg.client.world

import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.util.findTextures
import no.elg.infiniteBootleg.core.world.ContainerElement
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.Staff
import no.elg.infiniteBootleg.core.world.Tool
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion

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
