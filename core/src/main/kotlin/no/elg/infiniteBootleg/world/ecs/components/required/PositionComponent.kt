package no.elg.infiniteBootleg.world.ecs.components.required

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils
import ktx.ashley.Mapper
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world

data class PositionComponent(var x: Float, var y: Float) : Component {

  val blockX: Int = MathUtils.floor(x)
  val blockY: Int = MathUtils.floor(y)

  companion object : Mapper<PositionComponent>() {
    var Entity.position by propertyFor(mapper)

    val Entity.physicsX: Float
      get() = position.x + world.world.worldBody.worldOffsetX

    val Entity.physicsY: Float
      get() = position.y + world.world.worldBody.worldOffsetY
  }
}
