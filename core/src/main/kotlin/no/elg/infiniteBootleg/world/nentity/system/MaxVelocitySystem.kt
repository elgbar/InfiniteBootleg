package no.elg.infiniteBootleg.world.nentity.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.world.nentity.basicEntityFamily
import no.elg.infiniteBootleg.world.nentity.box2d
import no.elg.infiniteBootleg.world.nentity.velocity
import no.elg.infiniteBootleg.world.nentity.world
import kotlin.math.abs
import kotlin.math.sign

class MaxVelocitySystem : IteratingSystem(basicEntityFamily) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val velComp = entity.velocity
    val tooFastX: Boolean = abs(velComp.dx) > velComp.maxDx
    val tooFastY: Boolean = abs(velComp.dy) > velComp.maxDy

    if (tooFastX || tooFastY) {
      val nx = (if (tooFastX) sign(velComp.dx) * velComp.maxDx else velComp.dx)
      val ny = (if (tooFastY) sign(velComp.dy) * velComp.maxDy else velComp.dy)
      entity.world.world.postBox2dRunnable { entity.box2d.body.setLinearVelocity(nx, ny) }
    }
  }
}
