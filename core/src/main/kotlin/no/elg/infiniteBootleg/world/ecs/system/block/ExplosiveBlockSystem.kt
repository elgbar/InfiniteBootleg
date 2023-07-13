package no.elg.infiniteBootleg.world.ecs.system.block

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.blockEntityFamily

object ExplosiveBlockSystem : IteratingSystem(blockEntityFamily, UPDATE_PRIORITY_DEFAULT) {

  override fun processEntity(entity: Entity?, deltaTime: Float) {
  }
}
