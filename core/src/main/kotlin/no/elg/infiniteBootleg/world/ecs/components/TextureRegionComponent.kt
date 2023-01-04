package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.TextureRegion
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor

data class TextureRegionComponent(val texture: TextureRegion) : Component {
  companion object : Mapper<TextureRegionComponent>() {
    var Entity.textureRegion by propertyFor(mapper)
    var Entity.textureRegionOrNull by optionalPropertyFor(mapper)
  }
}
