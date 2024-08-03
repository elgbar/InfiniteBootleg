package no.elg.infiniteBootleg.world.box2d

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.utils.OrderedSet
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.api.Ticking
import no.elg.infiniteBootleg.util.CheckableDisposable
import no.elg.infiniteBootleg.util.FailureWatchdog
import no.elg.infiniteBootleg.world.BOX2D_LOCK
import no.elg.infiniteBootleg.world.chunks.Chunk.Companion.CHUNK_SIZE
import no.elg.infiniteBootleg.world.ticker.PostRunnableHandler
import no.elg.infiniteBootleg.world.ticker.WorldBox2DTicker.Companion.BOX2D_TIME_STEP
import no.elg.infiniteBootleg.world.world.World
import javax.annotation.concurrent.GuardedBy
import kotlin.math.abs
import com.badlogic.gdx.physics.box2d.World as Box2dWorld

private val logger = KotlinLogging.logger {}

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

  @field:Volatile
  private var disposed = false

  private val chunksToUpdate = OrderedSet<ChunkBody>().also { it.orderedItems().ordered = false }
  private val updatingChunks = OrderedSet<ChunkBody>().also { it.orderedItems().ordered = false }

  private val postRunnable = PostRunnableHandler()

  private val updatingChunksIterator = OrderedSet.OrderedSetIterator(updatingChunks)

  private val contactManager = ContactManager(world.engine)

  /**
   * Posts a [Runnable] on the physics thread.
   * The runnable will be executed under the synchronization of [BOX2D_LOCK].
   *
   * @see [com.badlogic.gdx.Application.postRunnable]
   */
  fun postBox2dRunnable(runnable: () -> Unit) = postRunnable.postRunnable(runnable)

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
        logger.error { "Trying to destroy an inactive body, the program will probably crash, userData: ${body.userData}" }
      }
      for (it in body.fixtureList.asSequence().map { it.userData }.filterIsInstance<Entity>()) {
        world.engine.removeEntity(it)
      }
      box2dWorld.destroyBody(body)
    }
  }

  private val box2dWatchdog = FailureWatchdog("step Box2D")
  private val ashleyWatchdog = FailureWatchdog("update ESC engine (ashley)")

  /**
   * Must not be under any locks of any kind (other than [BOX2D_LOCK]) when called
   */
  override fun tick() {
    if (disposed) {
      return
    }

    synchronized(chunksToUpdate) {
      updatingChunks.clear()
      updatingChunks.addAll(chunksToUpdate)
      chunksToUpdate.clear()
    }

    updatingChunksIterator.reset()

    synchronized(BOX2D_LOCK) {
      box2dWatchdog.watch {
        box2dWorld.step(BOX2D_TIME_STEP, 10, 10)
      }

      ashleyWatchdog.watch {
        world.engine.update(BOX2D_TIME_STEP)
      }

      postRunnable.executeRunnables()
      for (chunkBody in updatingChunksIterator) {
        if (chunkBody.shouldCreateBody()) {
          createBodyNow(chunkBody.bodyDef, chunkBody::onBodyCreated)
        }
      }
    }
  }

  /**
   * Query the world for all fixtures that potentially overlap the provided AABB.
   *
   * @param callback a user implemented callback class.
   * @param lowerX the x coordinate of the lower left corner
   * @param lowerY the y coordinate of the lower left corner
   * @param upperX the x coordinate of the upper right corner
   * @param upperY the y coordinate of the upper right corner
   * @param callback Called for each fixture found in the query AABB. return false to terminate the query.
   */
  fun queryFixtures(
    lowerX: Number,
    lowerY: Number,
    upperX: Number,
    upperY: Number,
    callback: ((Fixture) -> Boolean)
  ) {
    postBox2dRunnable {
      box2dWorld.QueryAABB(callback, lowerX.toFloat(), lowerY.toFloat(), upperX.toFloat(), upperY.toFloat())
    }
  }

  /**
   * Query the world for all entities that potentially overlap the provided AABB.
   *
   * @param callback a user implemented callback class.
   * @param lowerX the x coordinate of the lower left corner
   * @param lowerY the y coordinate of the lower left corner
   * @param upperX the x coordinate of the upper right corner
   * @param upperY the y coordinate of the upper right corner
   * @param callback Called for each entity found in the query AABB
   */
  fun queryEntities(
    lowerX: Number,
    lowerY: Number,
    upperX: Number,
    upperY: Number,
    callback: ((Set<Pair<Body, Entity>>) -> Unit)
  ) {
    postBox2dRunnable {
      val entities = mutableSetOf<Pair<Body, Entity>>()
      val queryCallback: (Fixture) -> Boolean = {
        val body = it.body
        (body.userData as? Entity)?.also { entity -> entities += body to entity }
        true
      }
      box2dWorld.QueryAABB(queryCallback, lowerX.toFloat(), lowerY.toFloat(), upperX.toFloat(), upperY.toFloat()) // blocking
      callback(entities)
    }
  }

  companion object {
    const val X_WORLD_GRAVITY = 0f
    const val Y_WORLD_GRAVITY = 2f * -9.8f

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
    synchronized(BOX2D_LOCK) {
      box2dWorld = Box2dWorld(Vector2(X_WORLD_GRAVITY, Y_WORLD_GRAVITY), true)
      box2dWorld.setContactListener(contactManager)
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
