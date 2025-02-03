package no.elg.infiniteBootleg.core.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.system.AuthoritativeSystem
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityComponent
import no.elg.infiniteBootleg.core.world.ecs.toFamily
import kotlin.math.abs

/**
 * Limit the velocity of a box2D body
 */
object MaxVelocitySystem : IteratingSystem(VelocityComponent::class.toFamily(), UPDATE_PRIORITY_DEFAULT), AuthoritativeSystem {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val velocity = entity.velocityComponent
    val tooFastX: Boolean = abs(velocity.dx) > velocity.maxDx
    val tooFastY: Boolean = abs(velocity.dy) > velocity.maxDy

    if (tooFastX || tooFastY) {
      entity.setVelocity(velocity.dx, velocity.dy)
    }
  }
}
