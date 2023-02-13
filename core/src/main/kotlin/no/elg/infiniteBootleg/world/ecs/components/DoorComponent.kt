package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.world.box2d.ObjectContactTracker

class DoorComponent : Component {

  val contacts = ObjectContactTracker<Entity>()
  val closed: Boolean get() = contacts.isEmpty

  companion object : Mapper<DoorComponent>() {
    var Entity.door by propertyFor(mapper)
    var Entity.doorOrNull by optionalPropertyFor(mapper)
  }
}
