package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import ktx.ashley.Mapper
import no.elg.infiniteBootleg.world.Material

data class MaterialComponent(val material: Material) : Component {
  companion object : Mapper<MaterialComponent>()
}
