package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.world.ecs.basicEntityFamily
import no.elg.infiniteBootleg.world.ecs.box2d
import no.elg.infiniteBootleg.world.ecs.world

class UpdatePositionSystem : IteratingSystem(basicEntityFamily) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val worldBody = entity.world.world.worldBody
    worldBody.postBox2dRunnable {
      val body = entity.box2d.body
    }
  }
}
