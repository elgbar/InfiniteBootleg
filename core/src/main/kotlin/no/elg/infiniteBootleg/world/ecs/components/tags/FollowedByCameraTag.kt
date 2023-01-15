package no.elg.infiniteBootleg.world.ecs.components.tags

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor

class FollowedByCameraTag : Component {
  companion object : Mapper<FollowedByCameraTag>() {
    var Entity.followedByCamera by tagFor<FollowedByCameraTag>()
  }
}
