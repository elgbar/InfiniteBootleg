package no.elg.infiniteBootleg.core.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.allOf
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_BEFORE_EVENTS
import no.elg.infiniteBootleg.core.world.ecs.buildAlive
import no.elg.infiniteBootleg.core.world.ecs.components.GroundedComponent
import no.elg.infiniteBootleg.core.world.ecs.components.GroundedComponent.Companion.groundedComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.compactBlockLoc

object ValidateGroundContactSystem : IteratingSystem(allOf(GroundedComponent::class, PositionComponent::class).buildAlive(), UPDATE_PRIORITY_BEFORE_EVENTS) {

  const val CUTOFF_DISTANCE: Double = 3.5 // todo should be based on entity size

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val grounded = entity.groundedComponent
    val pos = entity.compactBlockLoc
    grounded.validate(pos, CUTOFF_DISTANCE)
  }
}
