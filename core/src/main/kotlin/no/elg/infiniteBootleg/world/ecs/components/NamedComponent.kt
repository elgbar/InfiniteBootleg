package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor

data class NamedComponent(val name: String) : Component {
  companion object : Mapper<NamedComponent>() {
    var Entity.name by propertyFor(mapper)
    var Entity.nameOrNull by optionalPropertyFor(mapper)
  }
}
