package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocity
import no.elg.infiniteBootleg.world.ecs.components.transients.tags.UpdateBox2DVelocityTag.Companion.updateBox2DVelocity
import no.elg.infiniteBootleg.world.ecs.toFamily
import kotlin.math.abs

/**
 * Limit the velocity of a box2D body
 */
object MaxVelocitySystem : IteratingSystem(VelocityComponent::class.toFamily(), UPDATE_PRIORITY_DEFAULT) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val velocity = entity.velocity
    val tooFastX: Boolean = abs(velocity.dx) > velocity.maxDx
    val tooFastY: Boolean = abs(velocity.dy) > velocity.maxDy

    if (tooFastX || tooFastY) {
      entity.updateBox2DVelocity = true
      entity.velocity.dx = velocity.dx.coerceIn(-velocity.maxDx, velocity.maxDx)
      entity.velocity.dy = velocity.dy.coerceIn(-velocity.maxDy, velocity.maxDy)
    }
  }
}
