package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.physics.box2d.Body
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_LAST
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocity
import no.elg.infiniteBootleg.world.ecs.components.required.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.world.ecs.components.tags.UpdateBox2DPositionTag.updateBox2DPosition
import no.elg.infiniteBootleg.world.ecs.components.tags.UpdateBox2DVelocityTag.updateBox2DVelocity

/**
 * Write the position of the entity from the box2D entity
 */
object WriteBox2DStateSystem : IteratingSystem(basicDynamicEntityFamily, UPDATE_PRIORITY_LAST) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
//    val worldBody = entity.world.world.worldBody
//    worldBody.postBox2dRunnable {
    val body = entity.box2d.body
    updatePosition(entity, body)
    updateVelocity(entity, body)
//    }
  }

  private fun updatePosition(entity: Entity, body: Body) {
    if (entity.updateBox2DPosition) {
      entity.updateBox2DPosition = false

      body.position.x = entity.position.x
      body.position.y = entity.position.y
    }
  }

  private fun updateVelocity(entity: Entity, body: Body) {
    if (entity.updateBox2DVelocity) {
      entity.updateBox2DVelocity = false

      body.linearVelocity.x = entity.velocity.dx
      body.linearVelocity.y = entity.velocity.dy
    }
  }
}
