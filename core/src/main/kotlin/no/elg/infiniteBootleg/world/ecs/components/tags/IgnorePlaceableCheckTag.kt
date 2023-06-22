package no.elg.infiniteBootleg.world.ecs.components.tags

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor

class IgnorePlaceableCheckTag : Component {
  companion object : Mapper<IgnorePlaceableCheckTag>() {
    var Entity.ignorePlaceableCheck by tagFor<IgnorePlaceableCheckTag>()
  }
}
