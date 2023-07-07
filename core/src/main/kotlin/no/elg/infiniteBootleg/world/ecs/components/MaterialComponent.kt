package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.world.Material

data class MaterialComponent(val material: Material) : Component {
  companion object : Mapper<MaterialComponent>() {
    val Entity.material get() = materialComponent.material
    val Entity.materialOrNull get() = materialComponentOrNull?.material
    val Entity.materialComponent by propertyFor(mapper)
    var Entity.materialComponentOrNull by optionalPropertyFor(mapper)
  }
}
