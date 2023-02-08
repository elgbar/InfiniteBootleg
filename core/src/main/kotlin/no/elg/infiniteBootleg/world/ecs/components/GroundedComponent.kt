package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import ktx.collections.GdxLongArray

class GroundedComponent : Component {

  var contacts: GdxLongArray = GdxLongArray()
    private set

  operator fun plusAssign(toAdd: Long) {
    contacts.add(toAdd)
  }

  operator fun minusAssign(toRemove: Long) {
    contacts.removeValue(toRemove)
  }

  val onGround: Boolean get() = !contacts.isEmpty
//  val is: Boolean get() = !contacts.isEmpty

  companion object : Mapper<GroundedComponent>() {

    var Entity.grounded by propertyFor(mapper)
    var Entity.groundedOrNull by optionalPropertyFor(mapper)
  }
}
