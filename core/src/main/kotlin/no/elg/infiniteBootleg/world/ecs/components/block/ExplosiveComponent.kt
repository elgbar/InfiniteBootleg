package no.elg.infiniteBootleg.world.ecs.components.block

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor

class ExplosiveComponent : Component {
  companion object : Mapper<ExplosiveComponent>() {
    val Entity.explosiveComponent by propertyFor(ExplosiveComponent.mapper)
    var Entity.explosiveComponentOrNull by optionalPropertyFor(ExplosiveComponent.mapper)
  }
}
