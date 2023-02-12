package no.elg.infiniteBootleg.world.ecs.components.required

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.box2d.Body
import ktx.ashley.Mapper
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.CheckableDisposable
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world

/**
 *
 * @param box2dWidth  The height of this entity in box2d view
 * @param box2dHeight The width of this entity in box2d view
 */
class Box2DBodyComponent(body: Body, private val box2dWidth: Float, private val box2dHeight: Float) : Component, CheckableDisposable {
  private var internalBody: Body? = body
  private var disposed = false

  val halfBox2dWidth: Float get() = box2dWidth / 2f
  val halfBox2dHeight: Float get() = box2dHeight / 2f

  val worldWidth: Float get() = box2dWidth * Block.BLOCK_SIZE
  val worldHeight: Float get() = box2dHeight * Block.BLOCK_SIZE

  override val isDisposed get() = disposed

  val body: Body = (if (disposed) null else internalBody) ?: error("Tried to access a disposed body!")

  override fun dispose() {
    if (!isDisposed) {
      disposed = true
      val currentBody = internalBody ?: return
      this.internalBody = null
      val entity = currentBody.userData as Entity
      entity.world.world.worldBody.destroyBody(currentBody)
    }
  }

  companion object : Mapper<Box2DBodyComponent>() {
    val Entity.box2d by propertyFor(mapper)
  }
}
