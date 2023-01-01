package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import ktx.ashley.Mapper

data class AliveComponent(val health: Int, val maxHealth: Int) : Component {
  companion object : Mapper<AliveComponent>()
}
