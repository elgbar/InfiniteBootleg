package no.elg.infiniteBootleg.world.ecs.components.tags

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor

class GravityAffectedTag : Component {

  companion object : Mapper<GravityAffectedTag>() {
    var Entity.isAffectedByGravity by tagFor<GravityAffectedTag>()
  }
}
