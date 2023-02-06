package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.box2d.Fixture
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor

class GroundedComponent : Component {

  var contacts: Set<Fixture> = HashSet()
    private set

  operator fun plusAssign(toAdd: Fixture) {
    contacts += toAdd
  }

  operator fun minusAssign(toRemove: Fixture) {
    contacts += toRemove
  }

  val onGround: Boolean get() = contacts.isNotEmpty()

  companion object : Mapper<GroundedComponent>() {

    var Entity.grounded by propertyFor(mapper)
    var Entity.groundedOrNull by optionalPropertyFor(mapper)
  }
}
