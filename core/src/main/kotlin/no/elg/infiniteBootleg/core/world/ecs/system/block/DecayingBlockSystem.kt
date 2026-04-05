package no.elg.infiniteBootleg.core.world.ecs.system.block

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.system.AuthoritativeSystem
import no.elg.infiniteBootleg.core.world.ecs.components.DecayingComponent.Companion.decayComponent
import no.elg.infiniteBootleg.core.world.ecs.components.tags.BrokenBlockTag.Companion.brokenBlock
import no.elg.infiniteBootleg.core.world.ecs.decayingBlockFamily

object DecayingBlockSystem : IteratingSystem(decayingBlockFamily, UPDATE_PRIORITY_DEFAULT), AuthoritativeSystem {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val decayComp = entity.decayComponent
    decayComp.timeLeftSeconds -= deltaTime
    if (decayComp.timeLeftSeconds <= 0f) {
      entity.brokenBlock = true
    }
  }
}
