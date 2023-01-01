package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.box2d
import no.elg.infiniteBootleg.world.ecs.components.PositionComponent
import no.elg.infiniteBootleg.world.ecs.position
import no.elg.infiniteBootleg.world.ecs.world

object UpdatePositionSystem : IteratingSystem(basicDynamicEntityFamily) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val worldBody = entity.world.world.worldBody
    worldBody.postBox2dRunnable {
      val body = entity.box2d.body
      entity.position = PositionComponent(body.position.x.toDouble(), body.position.y.toDouble())
    }
  }
}
