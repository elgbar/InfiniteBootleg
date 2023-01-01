package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import ktx.ashley.Mapper

class ControlledComponent : Component {
  companion object : Mapper<ControlledComponent>()
}
