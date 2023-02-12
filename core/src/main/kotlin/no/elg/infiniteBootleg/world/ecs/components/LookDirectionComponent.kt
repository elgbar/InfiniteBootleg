package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.world.Direction

data class LookDirectionComponent(var direction: Direction) : Component {
  companion object : Mapper<LookDirectionComponent>() {
    var Entity.lookDirectionOrNull by optionalPropertyFor(LookDirectionComponent.mapper)
  }
}
