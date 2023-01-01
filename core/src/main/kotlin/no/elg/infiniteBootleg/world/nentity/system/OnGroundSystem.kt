package no.elg.infiniteBootleg.world.nentity.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.world.nentity.basicEntityFamily

object OnGroundSystem : IteratingSystem(basicEntityFamily) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    TODO("Not yet implemented")
  }
}
