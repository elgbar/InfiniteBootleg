package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.input.KeyboardControls.Companion.MAX_X_VEL
import no.elg.infiniteBootleg.input.KeyboardControls.Companion.MAX_Y_VEL
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.vector2f
import no.elg.infiniteBootleg.world.ecs.api.EntityParentLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.tags.UpdateBox2DVelocityTag.Companion.updateBox2DVelocity
import no.elg.infiniteBootleg.world.ecs.with

data class VelocityComponent(
  var dx: Float,
  var dy: Float
) : EntitySavableComponent {

  val maxDx: Float = MAX_X_VEL
  val maxDy: Float = MAX_Y_VEL

  fun toVector2f(): ProtoWorld.Vector2f = vector2f {
    x = this@VelocityComponent.dx
    y = this@VelocityComponent.dy
  }

  init {
    require(maxDx > 0) { "Max dx velocity must be strictly positive" }
    require(maxDy > 0) { "Max dy velocity must be strictly positive" }
  }

  override fun EntityKt.Dsl.save() {
    velocity = vector2f {
      x = dx
      y = dy
    }
  }

  companion object : EntityParentLoadableMapper<VelocityComponent>() {
    var Entity.velocity by propertyFor(mapper)
    var Entity.velocityOrNull by optionalPropertyFor(mapper)

    fun Entity.setVelocity(dx: Float, dy: Float) {
      velocityOrNull?.also {
        it.dx = dx.coerceIn(-it.maxDx, it.maxDx)
        it.dy = dy.coerceIn(-it.maxDy, it.maxDy)
        this.updateBox2DVelocity = true
      }
    }

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) {
      with(VelocityComponent(protoEntity.velocity.x, protoEntity.velocity.y))
    }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasVelocity()
  }
}
