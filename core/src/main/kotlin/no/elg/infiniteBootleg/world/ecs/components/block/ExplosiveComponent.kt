package no.elg.infiniteBootleg.world.ecs.components.block

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor

class ExplosiveComponent : Component {

  var fuse: Float = FUSE_DURATION_SECONDS
  val strength: Float = EXPLOSION_STRENGTH.toFloat()

  companion object : Mapper<ExplosiveComponent>() {
    val Entity.explosiveComponent by propertyFor(ExplosiveComponent.mapper)
    var Entity.explosiveComponentOrNull by optionalPropertyFor(ExplosiveComponent.mapper)

    /**
     * Randomness to what blocks are destroyed.
     *
     * Lower means more blocks destroyed, but more random holes around the crater.
     *
     * Higher means fewer blocks destroyed but less unconnected destroyed blocks. Note that too
     * large will not look good
     *
     * Minimum value should be above 3 as otherwise the edge of the explosion will clearly be
     * visible
     */
    const val RESISTANCE = 8
    const val FUSE_DURATION_SECONDS = 3f

    /** Maximum explosion radius  */
    const val EXPLOSION_STRENGTH = 40
  }
}
