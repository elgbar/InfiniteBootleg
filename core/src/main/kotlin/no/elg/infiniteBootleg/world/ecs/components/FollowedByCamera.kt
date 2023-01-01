package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import ktx.ashley.Mapper

class FollowedByCamera : Component {
  companion object : Mapper<FollowedByCamera>()
}
