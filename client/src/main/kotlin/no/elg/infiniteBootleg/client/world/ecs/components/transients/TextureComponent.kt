package no.elg.infiniteBootleg.client.world.ecs.components.transients

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.ClientComponent
import no.elg.infiniteBootleg.core.world.ecs.components.TextureRegionNameComponent.Companion.textureRegionNameComponent
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion

data class TextureComponent(val textureRegion: RotatableTextureRegion) : ClientComponent {
  override fun hudDebug(): String = "TextureComponent"

  companion object : Mapper<TextureComponent>() {
    var Entity.textureComponent by propertyFor(mapper)
    var Entity.textureComponentOrNull by optionalPropertyFor(mapper)

    val Entity.textureRegion: RotatableTextureRegion
      get() = textureComponentOrNull?.textureRegion ?: let {
        val textureRegion = ClientMain.inst().assets.findTexture(textureRegionNameComponent.textureName)
        textureComponent = TextureComponent(textureRegion)
        textureRegion
      }
  }
}
