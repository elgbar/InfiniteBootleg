package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.util.MAX_X_VEL
import no.elg.infiniteBootleg.core.util.MAX_Y_VEL
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.UpdateBox2DVelocityTag.Companion.updateBox2DVelocity
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.vector2f
import kotlin.math.absoluteValue

class VelocityComponent(dx: Float, dy: Float) : EntitySavableComponent {

  var dx: Float = dx
    private set
  var dy: Float = dy
    private set

  operator fun component1(): Float = dx
  operator fun component2(): Float = dy

  val maxDx: Float = MAX_X_VEL
  val maxDy: Float = MAX_Y_VEL

  fun toProtoVector2f(): ProtoWorld.Vector2f =
    vector2f {
      x = this@VelocityComponent.dx
      y = this@VelocityComponent.dy
    }

  fun isHorizontalStill(): Boolean = dx.absoluteValue < EFFECTIVE_ZERO
  fun isVerticalStill(): Boolean = dy.absoluteValue < EFFECTIVE_ZERO
  fun isStill(): Boolean = dx * dx + dy * dy < EFFECTIVE_ZERO * EFFECTIVE_ZERO
  fun isMoving(): Boolean = !isStill()

  init {
    require(maxDx > 0) { "Max dx velocity must be strictly positive" }
    require(maxDy > 0) { "Max dy velocity must be strictly positive" }
  }

  override fun EntityKt.Dsl.save() {
    velocity = toProtoVector2f()
  }

  fun toVector2(target: Vector2 = Vector2()): Vector2 = target.set(dx, dy)

  override fun hudDebug(): String = stringifyCompactLoc(dx, dy)

  /**
   * Set the entity velocity without updating the Box2D body. Use with caution.
   */
  fun setAshleyVelocity(newDx: Float, newDy: Float) {
    this.dx = newDx.coerceIn(-maxDx, maxDx)
    this.dy = newDy.coerceIn(-maxDy, maxDy)
  }

  companion object : EntityLoadableMapper<VelocityComponent>() {

    /**
     * Visually effective zero velocity threshold
     */
    const val EFFECTIVE_ZERO = 0.01f

    fun Entity.velocityOrNull(target: Vector2? = null): Vector2? = velocityComponentOrNull?.toVector2(target ?: Vector2())
    fun Entity.velocityOrZero(): Vector2 = velocityComponentOrNull?.toVector2(Vector2()) ?: Vector2.Zero
    fun Entity.velocityOrZero(target: Vector2 = Vector2()): Vector2 = velocityComponentOrNull?.toVector2(target) ?: target.set(0f, 0f)

    var Entity.velocityComponent by propertyFor(mapper)
    var Entity.velocityComponentOrNull by optionalPropertyFor(mapper)

    inline fun Entity.setVelocity(velocity: ProtoWorld.Vector2f) = setVelocity(velocity.x, velocity.y)
    inline fun Entity.setVelocity(velocity: Vector2) = setVelocity(velocity.x, velocity.y)
    fun Entity.setVelocity(dx: Float, dy: Float) {
      ThreadType.PHYSICS.requireCorrectThreadType { "Setting entity velocity can only be done on the physics thread" }
      velocityComponentOrNull?.also {
        it.setAshleyVelocity(dx, dy)
        updateBox2DVelocity = true
      }
    }

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) = safeWith { VelocityComponent(protoEntity.velocity.x, protoEntity.velocity.y) }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasVelocity()
  }
}
