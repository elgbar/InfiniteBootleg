package no.elg.infiniteBootleg.world.ecs.components.tags

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor

class FlyingTag : Component {
  companion object : Mapper<FlyingTag>() {
    var Entity.flying by tagFor<FlyingTag>()
  }
}
