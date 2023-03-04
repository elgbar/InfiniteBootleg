package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor

data class NamedComponent(val name: String) : Component {
  companion object : Mapper<NamedComponent>() {
    val Entity.name get() = nameComponent.name
    val Entity.nameOrNull get() = nameComponentOrNull?.name
    var Entity.nameComponent by propertyFor(mapper)
    var Entity.nameComponentOrNull by optionalPropertyFor(mapper)
  }
}
