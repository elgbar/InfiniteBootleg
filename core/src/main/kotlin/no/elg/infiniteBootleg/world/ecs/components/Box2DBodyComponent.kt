package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.physics.box2d.Body
import ktx.ashley.Mapper

data class Box2DBodyComponent(val body: Body) : Component {
  companion object : Mapper<Box2DBodyComponent>()
}
