package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.box2d
import no.elg.infiniteBootleg.world.ecs.velocity
import no.elg.infiniteBootleg.world.ecs.world
import kotlin.math.abs
import kotlin.math.sign

object MaxVelocitySystem : IteratingSystem(basicDynamicEntityFamily) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val velComp = entity.velocity
    val tooFastX: Boolean = abs(velComp.dx) > velComp.maxDx
    val tooFastY: Boolean = abs(velComp.dy) > velComp.maxDy

    if (tooFastX || tooFastY) {
      val nx = (if (tooFastX) sign(velComp.dx) * velComp.maxDx else velComp.dx)
      val ny = (if (tooFastY) sign(velComp.dy) * velComp.maxDy else velComp.dy)
      entity.world.world.postBox2dRunnable { entity.box2d.body.setLinearVelocity(nx.toFloat(), ny.toFloat()) }
    }
  }
}
