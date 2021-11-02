package no.elg.infiniteBootleg.world.box2d

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.utils.Array
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.Ticking
import no.elg.infiniteBootleg.world.Block.BLOCK_SIZE
import no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.render.WorldRender
import no.elg.infiniteBootleg.world.render.WorldRender.BOX2D_LOCK
import no.elg.infiniteBootleg.world.subgrid.contact.ContactManager
import kotlin.math.abs
import com.badlogic.gdx.physics.box2d.World as Box2dWorld

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
  lateinit var box2dWorld: Box2dWorld

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
  fun destroyBody(body: Body) {
    synchronized(BOX2D_LOCK) {
      require(!box2dWorld.isLocked) {
        "Cannot destroy body when box2d world is locked, to fix this schedule the destruction either sync or async, userData: ${body.userData}"
      }
      if (!body.isActive) {
        Main.logger().error("BOX2D", "Trying to destroy an inactive body, the program will probably crash, userData: ${body.userData}")
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
    if (!Settings.client) {
      // We only short because of the light, no point is shifting when there is no client
      return
    }

    if (Main.isSingleplayer()) {
      // TODO move to WorldRender, makes more sense to do this there
      synchronized(BOX2D_LOCK) {
        val player = ClientMain.inst().player ?: return
        if (player.isInvalid) {
          return
        }

        val physicsPosition = player.physicsPosition
        val shiftX = calculateShift(physicsPosition.x)
        val shiftY = calculateShift(physicsPosition.y)

        if (shiftX == 0f && shiftY == 0f) {
          // Still in-bounds
          return
        }
        // the toShift method assumes no offset, so we must subtract the old offset from the new
        shiftWorldOffset(shiftX, shiftY)
        Main.logger().debug("BOX2D", "Shifting world offset by ($shiftX, $shiftY) now ($worldOffsetX, $worldOffsetY)")
      }
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
      if (rayHandler != null) {
        for (light in rayHandler.enabledLights) {
          light.position = light.position.add(deltaOffsetX, deltaOffsetY)
        }
        // TODO enable if needed
//      for (light in rayHandler.disabledLights) {
//        light.position = light.position.add(deltaOffsetX, deltaOffsetY)
//      }
        rayHandler.update()
      }

      // test logic only, move to world render when possible
      world.render.camera?.translate(deltaOffsetX * BLOCK_SIZE, deltaOffsetY * BLOCK_SIZE, 0f)
      world.render.update()
    }
  }

  /**
   * 	@param callback Called for each fixture found in the query AABB. return false to terminate the query.
   */
  fun queryAABB(worldX: Float, worldY: Float, worldWidth: Float, worldHeight: Float, callback: ((Fixture) -> Boolean)) {
    synchronized(BOX2D_LOCK) {
      box2dWorld.QueryAABB(callback, worldX + worldOffsetX, worldY + worldOffsetY, worldWidth, worldHeight)
    }
  }

  companion object {
    const val X_WORLD_GRAVITY = 0f
    const val Y_WORLD_GRAVITY = -20f

    const val WORLD_MOVE_OFFSET_THRESHOLD = CHUNK_SIZE * 8f

    /**
     * Given the distance from the current physics origin, calculate how much the physics world offset should be shifted.
     *
     * Simply put this calculates how much we must move the world origin to stay with a distance of [WORLD_MOVE_OFFSET_THRESHOLD].
     *
     * @param physicsCoordinate Distance from the current physics origin
     * @return How much we should shift the world origin by. If `0` we are within bounds.
     */
    @JvmStatic
    fun calculateShift(physicsCoordinate: Float): Float {
      val sign = when {
        physicsCoordinate > WORLD_MOVE_OFFSET_THRESHOLD -> -1
        physicsCoordinate < -WORLD_MOVE_OFFSET_THRESHOLD -> 1
        else -> return 0f // Make no change
      }
      return sign * ((abs(physicsCoordinate) / WORLD_MOVE_OFFSET_THRESHOLD).toInt() * WORLD_MOVE_OFFSET_THRESHOLD)
    }
  }

  init {
    if (Main.inst().isNotTest) {
      synchronized(BOX2D_LOCK) {
        box2dWorld = Box2dWorld(Vector2(X_WORLD_GRAVITY, Y_WORLD_GRAVITY), true)
        box2dWorld.setContactListener(ContactManager())
        timeStep = world.worldTicker.secondsDelayBetweenTicks
      }
    }
  }
}
