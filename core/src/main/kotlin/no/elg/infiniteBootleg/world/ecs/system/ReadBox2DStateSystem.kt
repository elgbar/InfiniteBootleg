package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.physics.box2d.Body
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_EARLY
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocityOrNull
import no.elg.infiniteBootleg.world.ecs.components.required.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position

/**
 * Read the position of the entity from the box2D entity
 */
object ReadBox2DStateSystem : IteratingSystem(basicDynamicEntityFamily, UPDATE_PRIORITY_EARLY) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
//    val worldBody = entity.world.world.worldBody
//    worldBody.postBox2dRunnable {
    val body = entity.box2d.body
    readPosition(entity, body)
    readVelocity(entity, body)
//    }
  }

  private fun readPosition(entity: Entity, body: Body) {
    entity.position.x = body.position.x
    entity.position.y = body.position.y
  }

  private fun readVelocity(entity: Entity, body: Body) {
    val velocity = entity.velocityOrNull ?: return
    velocity.dx = body.linearVelocity.x
    velocity.dy = body.linearVelocity.y
  }
}
