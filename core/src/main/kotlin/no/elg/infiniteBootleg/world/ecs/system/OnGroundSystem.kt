package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.world.ecs.basicEntityFamily

object OnGroundSystem : IteratingSystem(basicEntityFamily) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    TODO("Not yet implemented")
  }
}
