package no.elg.infiniteBootleg.world.box2d

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.utils.Array.ArrayIterator
import com.badlogic.gdx.utils.OrderedSet
import ktx.collections.GdxArray
import no.elg.infiniteBootleg.CheckableDisposable
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.Ticking
import no.elg.infiniteBootleg.world.BOX2D_LOCK
import no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.subgrid.contact.ContactManager
import no.elg.infiniteBootleg.world.ticker.WorldBox2DTicker.Companion.BOX2D_TPS_DIVIDER
import javax.annotation.concurrent.GuardedBy
import kotlin.math.abs
import com.badlogic.gdx.physics.box2d.World as Box2dWorld

/**
 * Wrapper for [com.badlogic.gdx.physics.box2d.World] for asynchronous reasons
 *
 * @author Elg
 */
open class WorldBody(private val world: World) : Ticking, CheckableDisposable {

  /**
   * Use the returned object with care,
   *
   *
   * Synchronized over [BOX2D_LOCK] when it must be used
   *
   * @return The underlying box2d world
   */
  @GuardedBy("BOX2D_LOCK")
  lateinit var box2dWorld: Box2dWorld

  private var timeStep = 0f
  var worldOffsetX = 0f
    private set
  var worldOffsetY = 0f
    private set

  @field:Volatile
  private var disposed = false

  private val bodies = GdxArray<Body>()

  private val chunksToUpdate = OrderedSet<ChunkBody>().also { it.orderedItems().ordered = false }
  private val updatingChunks = OrderedSet<ChunkBody>().also { it.orderedItems().ordered = false }

  private val runnables = GdxArray<Runnable>()
  private val executedRunnables = GdxArray<Runnable>()

  private val updatingChunksIterator = OrderedSet.OrderedSetIterator(updatingChunks)
  private val executedRunnablesIterator = ArrayIterator(executedRunnables)
  private val bodiesIterator = ArrayIterator(bodies)

  /**
   * Posts a [Runnable] on the physics thread.
   * The runnable will be executed under the synchronization of [BOX2D_LOCK].
   *
   * @see [com.badlogic.gdx.Application.postRunnable]
   */
  fun postBox2dRunnable(runnable: Runnable) {
    synchronized(runnables) {
      runnables.add(runnable)
    }
  }

  internal fun updateChunk(chunkBody: ChunkBody) {
    synchronized(chunksToUpdate) {
      chunksToUpdate.add(chunkBody)
    }
  }

  /**
   * Create a new body in this world, this method can be called from any thread
   *
   * Must not be under any locks of any kind (other than [BOX2D_LOCK]) when called
   *
   * @param def
   * The definition of the body to create
   */
  fun createBody(def: BodyDef, callback: (Body) -> Unit) {
    postBox2dRunnable {
      createBodyNow(def, callback)
    }
  }

  @GuardedBy("BOX2D_LOCK")
  private fun createBodyNow(def: BodyDef, callback: (Body) -> Unit) {
    val body = box2dWorld.createBody(def)
    applyShift(body, worldOffsetX, worldOffsetY)
    callback(body)
  }

  /**
   * Destroy the given body, this method can be called from any thread
   *
   * @param body
   * The body to destroy
   */
  fun destroyBody(body: Body) {
    postBox2dRunnable {
      require(!box2dWorld.isLocked) {
        "Cannot destroy body when box2d world is locked, to fix this schedule the destruction either sync or async, userData: ${body.userData}"
      }
      if (!body.isActive) {
        Main.logger().error("BOX2D", "Trying to destroy an inactive body, the program will probably crash, userData: ${body.userData}")
      }
      box2dWorld.destroyBody(body)
    }
  }

  /**
   * Must not be under any locks of any kind (other than [BOX2D_LOCK]) when called
   */
  override fun tick() {
    if (disposed) {
      return
    }

    synchronized(runnables) {
      executedRunnables.clear()
      executedRunnables.addAll(runnables)
      runnables.clear()
    }

    synchronized(chunksToUpdate) {
      updatingChunks.clear()
      updatingChunks.addAll(chunksToUpdate)
      chunksToUpdate.clear()
    }

    executedRunnablesIterator.reset()
    updatingChunksIterator.reset()

    synchronized(BOX2D_LOCK) {
      box2dWorld.step(timeStep, 8, 4)
      world.engine.update(timeStep)

      for (runnable in executedRunnablesIterator) {
        runnable.run()
      }
      for (chunkBody in updatingChunksIterator) {
        if (chunkBody.shouldCreateBody()) {
          createBodyNow(chunkBody.bodyDef, chunkBody::onBodyCreated)
        }
      }
    }
  }

  /**
   * Must not be under any locks of any kind (other than [BOX2D_LOCK]) when called
   */
  override fun tickRare() {
    if (Main.isServer()) {
      // We only short because of the light, no point is shifting when there is no client
      return
    }

    if (Main.isSingleplayer()) {
      synchronized(BOX2D_LOCK) {
        if (disposed) {
          return@synchronized
        }
//        val player = ClientMain.inst().player ?: return
//        if (player.isDisposed) {
//          return
//        }

//        val physicsPosition = player.physicsPosition
//        val shiftX = calculateShift(physicsPosition.x)
//        val shiftY = calculateShift(physicsPosition.y)

//        if (shiftX == 0f && shiftY == 0f) {
//          // Still in-bounds
//          return
//        }
        // the toShift method assumes no offset, so we must subtract the old offset from the new
//        shiftWorldOffset(shiftX, shiftY)
//        Main.logger().debug("BOX2D", "Shifting world offset by ($shiftX, $shiftY) now ($worldOffsetX, $worldOffsetY)")
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
    postBox2dRunnable {
      worldOffsetX += deltaOffsetX
      worldOffsetY += deltaOffsetY
      bodies.clear()
      bodies.ensureCapacity(world.engine.entities.size())
      box2dWorld.getBodies(bodies)
      bodiesIterator.reset()
      for (body in bodiesIterator) {
        applyShift(body, deltaOffsetX, deltaOffsetY)
      }

      world.render.update()
    }
  }

  /**
   * 	@param callback Called for each fixture found in the query AABB. return false to terminate the query.
   */
  fun queryAABB(worldX: Float, worldY: Float, worldWidth: Float, worldHeight: Float, callback: ((Fixture) -> Boolean)) {
    postBox2dRunnable {
      box2dWorld.QueryAABB(callback, worldX + worldOffsetX, worldY + worldOffsetY, worldWidth, worldHeight)
    }
  }

  companion object {
    const val X_WORLD_GRAVITY = 0f
    const val Y_WORLD_GRAVITY = -20f

    const val WORLD_MOVE_OFFSET_THRESHOLD: Float = CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE.toFloat()

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
        box2dWorld.setContactListener(ContactManager(world.engine))
        timeStep = (1f / Settings.tps) * BOX2D_TPS_DIVIDER
      }
    }
  }

  override val isDisposed: Boolean
    get() = disposed

  override fun dispose() {
    synchronized(BOX2D_LOCK) {
      if (disposed) {
        return
      }
      disposed = true
      box2dWorld.dispose()
    }
  }
}
