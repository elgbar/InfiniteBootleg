package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import ktx.ashley.Mapper

data class PositionComponent(val x: Double, val y: Double) : Component {

//  fun getPhysicsPosition(): Vector2 {
//    return .add(world.getWorldBody().worldOffsetX, world.getWorldBody().worldOffsetY)
//  }

  companion object : Mapper<PositionComponent>()
}
