package no.elg.infiniteBootleg.world.ecs.components.transients

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.world.render.RotatableTextureRegion

data class TextureRegionComponent(var texture: RotatableTextureRegion) : Component {
  companion object : Mapper<TextureRegionComponent>() {
    var Entity.textureRegion by propertyFor(mapper)
    var Entity.textureRegionOrNull by optionalPropertyFor(mapper)
  }
}
