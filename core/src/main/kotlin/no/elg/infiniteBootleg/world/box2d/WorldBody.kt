package no.elg.infiniteBootleg.world.box2d

import com.badlogic.gdx.physics.box2d.World as Box2dWorld
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.utils.Array
import kotlin.math.abs
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Ticking
import no.elg.infiniteBootleg.world.Block.BLOCK_SIZE
import no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.render.WorldRender
import no.elg.infiniteBootleg.world.render.WorldRender.BOX2D_LOCK
import no.elg.infiniteBootleg.world.subgrid.contact.ContactManager

/**
 * Wrapper for [com.badlogic.gdx.physics.box2d.World1] for asynchronous reasons
 *
 * @author Elg
 */
class WorldBody(private val world: World) : Ticking {

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
  var worldOffsetX = 0f
    private set
  var worldOffsetY = 0f
    private set

  private val bodies = Array<Body>()

  /**
   * Create a new body in this world, this method can be called from any thread
   *
   * @param def
   * The definition of the body to create
   */
  fun createBody(def: BodyDef): Body {
    synchronized(BOX2D_LOCK) {
      val body = box2dWorld.createBody(def)
      applyShift(body, worldOffsetX, worldOffsetY)
      return body
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
      require(!box2dWorld.isLocked) {
        "Cannot destroy body when box2d world is locked, to fix this schedule the destruction either sync or async"
      }
      if (!body.isActive) {
        Main.logger().error("BOX2D", "Trying to destroy an inactive body, the program will probably crash")
      }
      box2dWorld.destroyBody(body)
    }
  }

  override fun tick() {
    synchronized(BOX2D_LOCK) {
      box2dWorld.step(timeStep, 20, 10)
    }
  }

  override fun tickRare() {
    val player = Main.inst().player ?: return
    synchronized(BOX2D_LOCK) {

      fun toShift(current: Float): Int {
        val sign = when {
          current > WORLD_MOVE_OFFSET_THRESHOLD -> -1
          current < -WORLD_MOVE_OFFSET_THRESHOLD -> 1
          else -> return 0
        }
        return sign * ((abs(current) / WORLD_MOVE_OFFSET_THRESHOLD).toInt() * WORLD_MOVE_OFFSET_THRESHOLD)
      }

      val physicsPosition = player.physicsPosition
      val shiftX = toShift(physicsPosition.x)
      val shiftY = toShift(physicsPosition.y)

      if (shiftX == 0 && shiftY == 0) {
        //Still in-bounds
        return
      }
      Main.logger().debug("BOX2D", "Shifting world offset by $shiftX, $shiftY")
      shiftWorldOffset(shiftX.toFloat(), shiftY.toFloat())
    }
  }

  private fun applyShift(body: Body, deltaOffsetX: Float, deltaOffsetY: Float) {
    val old = body.transform
    body.setTransform(old.position.x + deltaOffsetX, old.position.y + deltaOffsetY, old.rotation)
  }

  /**
   * Move world offset to make sure physics don't go haywire by floating point rounding error
   */
  fun shiftWorldOffset(deltaOffsetX: Float, deltaOffsetY: Float) {
    synchronized(BOX2D_LOCK) {
      worldOffsetX += deltaOffsetX
      worldOffsetY += deltaOffsetY
      bodies.clear()
      bodies.ensureCapacity(world.entities.size)
      box2dWorld.getBodies(bodies)
      for (body in bodies) {
        applyShift(body, deltaOffsetX, deltaOffsetY)
      }
      for (entity in world.entities) {
        entity.updatePos()
      }

      val rayHandler = world.render.rayHandler
      for (light in rayHandler.enabledLights) {
        println("o offset ${light.position}")
        light.position = light.position.add(deltaOffsetX, deltaOffsetY)
        println("n offset ${light.position}")
      }
      //TODO enable if needed
//      for (light in rayHandler.disabledLights) {
//        light.position = light.position.add(deltaOffsetX, deltaOffsetY)
//      }
      rayHandler.update()


      //test logic only, move to world render when possible
      world.render.camera.translate(deltaOffsetX * BLOCK_SIZE, deltaOffsetY * BLOCK_SIZE, 0f)
      world.render.update()
    }
  }

  companion object {
    const val X_WORLD_GRAVITY = 0f
    const val Y_WORLD_GRAVITY = -20f

    const val WORLD_MOVE_OFFSET_THRESHOLD = CHUNK_SIZE * 8
  }

  init {
    synchronized(BOX2D_LOCK) {
      box2dWorld = Box2dWorld(Vector2(X_WORLD_GRAVITY, Y_WORLD_GRAVITY), true)
      box2dWorld.setContactListener(ContactManager(world))
      timeStep = world.worldTicker.secondsDelayBetweenTicks
    }
  }
}
