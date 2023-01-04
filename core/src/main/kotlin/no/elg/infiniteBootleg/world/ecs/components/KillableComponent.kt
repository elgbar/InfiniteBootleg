package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor

data class KillableComponent(val maxHealth: Int = 10, val health: Int = maxHealth) : Component {

  val dead: Boolean = health <= 0
  val alive: Boolean = health > 0

  companion object : Mapper<KillableComponent>() {
    var Entity.killable by propertyFor(mapper)
    var Entity.killableOrNull by optionalPropertyFor(mapper)
  }
}
