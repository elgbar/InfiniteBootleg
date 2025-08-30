package no.elg.infiniteBootleg.client.world

import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.rotatableTextureName
import no.elg.infiniteBootleg.core.world.ContainerElement
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.Staff
import no.elg.infiniteBootleg.core.world.TexturedContainerElement
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion

val ContainerElement.textureRegion: RotatableTextureRegion?
  get() {
    return when (this) {
      is Staff -> ClientMain.inst().assets.staffTexture
      is TexturedContainerElement -> findTextures()
      is Material -> null
    }
  }

private fun TexturedContainerElement.findTextures(): RotatableTextureRegion =
  if (Main.isServer) {
    serverRotatableTextureRegion()
  } else {
    ClientMain.inst().assets.findTextureOrNull(rotatableTextureName(textureName), rotationAllowed = true)
      ?: ClientMain.inst().assets.findTextureOrNull(textureName, rotationAllowed = false)
      ?: serverRotatableTextureRegion()
  }

private fun TexturedContainerElement.serverRotatableTextureRegion(): RotatableTextureRegion = RotatableTextureRegion(null, false, textureName)
