package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.physics.box2d.BodyDef
import ktx.ashley.Mapper

data class Box2DBodyDefComponent(val bodyDef: BodyDef = DEFAULT) : Component {
  companion object : Mapper<Box2DBodyDefComponent>() {
    val DEFAULT by lazy {
      BodyDef().also {
        it.type = BodyDef.BodyType.DynamicBody
        it.linearDamping = 1f
        it.fixedRotation = true
      }
    }
  }
}
