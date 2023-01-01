package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.graphics.g2d.TextureRegion
import ktx.ashley.Mapper

data class TextureRegionComponent(val texture: TextureRegion) : Component {
  companion object : Mapper<TextureRegionComponent>()
}
