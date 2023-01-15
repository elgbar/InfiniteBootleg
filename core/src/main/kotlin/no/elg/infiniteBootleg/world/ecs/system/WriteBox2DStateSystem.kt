package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_LAST
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocity
import no.elg.infiniteBootleg.world.ecs.components.required.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.world.ecs.components.tags.UpdateBox2DPositionTag.Companion.updateBox2DPosition
import no.elg.infiniteBootleg.world.ecs.components.tags.UpdateBox2DVelocityTag.Companion.updateBox2DVelocity

/**
 * Write the position of the entity from the box2D entity
 */
object WriteBox2DStateSystem : IteratingSystem(basicDynamicEntityFamily, UPDATE_PRIORITY_LAST) {

  private val tmp = Vector2()

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
      body.setLinearVelocity(entity.position.x, entity.position.y)
    }
  }

  private fun updateVelocity(entity: Entity, body: Body) {
    if (entity.updateBox2DVelocity) {
      entity.updateBox2DVelocity = false

      tmp.x = entity.velocity.dx
      tmp.y = entity.velocity.dy
      body.setTransform(tmp, 0f)
    }
  }
}
