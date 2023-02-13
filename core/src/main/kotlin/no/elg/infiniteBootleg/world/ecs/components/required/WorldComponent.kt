package no.elg.infiniteBootleg.world.ecs.components.required

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.world.World

data class WorldComponent(val world: World) : Component {
  companion object : Mapper<WorldComponent>() {
    val Entity.world by propertyFor(mapper)
  }
}