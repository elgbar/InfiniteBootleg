package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import ktx.ashley.Mapper

data class NamedComponent(val name: String) : Component {
  companion object : Mapper<NamedComponent>()
}
