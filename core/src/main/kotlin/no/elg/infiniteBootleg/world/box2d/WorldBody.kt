package no.elg.infiniteBootleg.world.box2d

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import no.elg.infiniteBootleg.Ticking
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.render.WorldRender
import no.elg.infiniteBootleg.world.render.WorldRender.BOX2D_LOCK
import no.elg.infiniteBootleg.world.subgrid.contact.ContactManager
import com.badlogic.gdx.physics.box2d.World as Box2dWorld

/**
 * Wrapper for [com.badlogic.gdx.physics.box2d.World1] for asynchronous reasons
 *
 * @author Elg
 */
class WorldBody(world: World) : Ticking {

  /**
   * Use the returned object with care,
   *
   *
   * Synchronized over [WorldRender.BOX2D_LOCK] when it must be used
   *
   * @return The underlying box2d world
   */
  val box2dWorld: Box2dWorld

  private var timeStep = 0f

  /**
   * Create a new body in this world, this method can be called from any thread
   *
   * @param def
   * The definition of the body to create
   */
  fun createBody(def: BodyDef): Body {
    synchronized(BOX2D_LOCK) {
      return box2dWorld.createBody(def)
    }
  }

  /**
   * Destroy the given body, this method can be called from any thread
   *
   * @param body
   * The body to destroy
   */
  fun destroyBody(body: Body?) {
    if (body == null) {
      return
    }
    synchronized(BOX2D_LOCK) {
      box2dWorld.destroyBody(body)
    }
  }

  override fun tick() {
    synchronized(BOX2D_LOCK) {
      box2dWorld.step(timeStep, 20, 10)
    }
  }

  companion object {
    const val X_WORLD_GRAVITY = 0f
    const val Y_WORLD_GRAVITY = -20f
  }

  init {
    synchronized(BOX2D_LOCK) {
      box2dWorld = Box2dWorld(Vector2(X_WORLD_GRAVITY, Y_WORLD_GRAVITY), true)
      box2dWorld.setContactListener(ContactManager(world))
      timeStep = world.worldTicker.secondsDelayBetweenTicks
    }
  }
}
