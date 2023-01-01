package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import ktx.ashley.Mapper
import no.elg.infiniteBootleg.world.World

data class WorldComponent(val world: World) : Component {
  companion object : Mapper<WorldComponent>()
}
