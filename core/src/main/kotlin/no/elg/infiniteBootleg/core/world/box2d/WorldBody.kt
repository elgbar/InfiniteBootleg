package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.structs.b2BodyDef
import com.badlogic.gdx.box2d.structs.b2BodyId
import com.badlogic.gdx.box2d.structs.b2WorldId
import com.google.errorprone.annotations.concurrent.GuardedBy
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.api.Ticking
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.util.CheckableDisposable
import no.elg.infiniteBootleg.core.util.Compacted2Float
import no.elg.infiniteBootleg.core.util.EntityFlags.INVALID_FLAG
import no.elg.infiniteBootleg.core.util.EntityFlags.enableFlag
import no.elg.infiniteBootleg.core.util.FailureWatchdog
import no.elg.infiniteBootleg.core.util.WorldCompactLoc
import no.elg.infiniteBootleg.core.util.isBeingRemoved
import no.elg.infiniteBootleg.core.world.BOX2D_LOCK
import no.elg.infiniteBootleg.core.world.ticker.PostRunnableHandler
import no.elg.infiniteBootleg.core.world.ticker.WorldBox2DTicker.Companion.BOX2D_SUB_STEP_COUNT
import no.elg.infiniteBootleg.core.world.ticker.WorldBox2DTicker.Companion.BOX2D_TIME_STEP
import no.elg.infiniteBootleg.core.world.world.World

private val logger = KotlinLogging.logger {}

/**
 * Wrapper for [com.badlogic.gdx.physics.box2d.World] for asynchronous reasons
 *
 * @author Elg
 */
open class WorldBody(private val world: World) :
  Ticking,
  CheckableDisposable {

  init {
    synchronized(BOX2D_LOCK) {
      val worldDef = Box2d.b2DefaultWorldDef()
      worldDef.gravity().apply {
        x(X_WORLD_GRAVITY)
        y(Y_WORLD_GRAVITY)
      }
      box2dWorld = Box2d.b2CreateWorld(worldDef.asPointer())
      Box2d.b2World_IsValid(box2dWorld).also { isValid ->
        if (!isValid) {
          throw IllegalStateException("Box2D world is not valid")
        }
      }
//      box2dWorld.setContactListener(contactManager)
    }
  }

  /**
   * Use the returned object with care,
   *
   *
   * Synchronized over [BOX2D_LOCK] when it must be used
   *
   * @return The underlying box2d world
   */
  @GuardedBy("BOX2D_LOCK")
  val box2dWorld: b2WorldId

  @field:Volatile
  private var disposed = false

  private val postRunnable = PostRunnableHandler()

//  private val contactManager = ContactManager(world.engine)

  /**
   * Posts a [Runnable] on the physics thread.
   * The runnable will be executed under the synchronization of [BOX2D_LOCK].
   *
   * @see [com.badlogic.gdx.Application.postRunnable]
   */
  fun postBox2dRunnable(runnable: () -> Unit) = postRunnable.postRunnable(runnable)

  internal fun updateChunk(chunkBody: ChunkBody) {
    postRunnable.postRunnable {
      if (chunkBody.shouldCreateBody()) {
        createBodyNow(chunkBody.bodyDef, chunkBody::onBodyCreated)
      }
    }
  }

  /**
   * Thread safe and fastest way to remove an entity from the world
   */
  internal fun removeEntity(entity: Entity) {
    if (entity.isBeingRemoved) {
      return
    }
    entity.enableFlag(INVALID_FLAG)
    if (ThreadType.Companion.isCurrentThreadType(ThreadType.PHYSICS)) {
      // OK to remove at once since this is the only thread we can remove entities from
      world.engine.removeEntity(entity)
    } else {
      postRunnable.postRunnable { world.engine.removeEntity(entity) }
    }
  }

  /**
   * Create a new body in this world, this method can be called from any thread
   *
   * Must not be under any locks of any kind (other than [BOX2D_LOCK]) when called
   *
   * @param def The definition of the body to create
   */
  fun createBody(def: b2BodyDef, callback: (b2BodyId) -> Unit) {
    postBox2dRunnable {
      createBodyNow(def, callback)
    }
  }

  @GuardedBy("BOX2D_LOCK")
  private fun createBodyNow(def: b2BodyDef, callback: (b2BodyId) -> Unit) {
    val body: b2BodyId = Box2d.b2CreateBody(box2dWorld, def.asPointer())
    callback(body)
//    Box2d.b2Body_SetUserData() // https://github.com/libgdx/gdx-box2d/blob/master/README.md#working-with-voidpointer-context-or-user-data
//    val userData = body.userData
//    if (userData == null) {
//      IllegalAction.STACKTRACE.handle { "Userdata not added when creating body" }
//    }
  }

  /**
   * Destroy the given body, this method can be called from any thread
   *
   * @param bodyId
   * The body to destroy
   */
  fun destroyBody(bodyId: b2BodyId) {
    postBox2dRunnable {
      require(Box2d.b2Body_IsValid(bodyId)) {
        "Cannot destroy body that is not valid" // , userData: ${body.userData}"
      }
//      require(!box2dWorld.isLocked) {
//        "Cannot destroy body when box2d world is locked, to fix this schedule the destruction either sync or async, userData: ${body.userData}"
//      }
//      if (!body.isActive) {
//        logger.error { "Trying to destroy an inactive body, the program will probably crash, userData: ${body.userData}" }
//      }

      // TODO do cleanup entities when the body is destroyed
//      body.fixtureList.asSequence().map { it.userData }.filterIsInstance<Entity>().forEach { world.removeEntity(it, DespawnReason.CHUNK_UNLOADED) }
      Box2d.b2DestroyBody(bodyId)
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
    synchronized(BOX2D_LOCK) {
      box2dWatchdog.watch {
        Box2d.b2World_Step(box2dWorld, BOX2D_TIME_STEP, BOX2D_SUB_STEP_COUNT)
      }

      ashleyWatchdog.watch {
        world.engine.update(BOX2D_TIME_STEP)
      }

      postRunnable.executeRunnables()
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
    callback: ((Set<Pair<b2BodyId, Entity>>) -> Unit)
  ) {
    postBox2dRunnable {
      val entities = mutableSetOf<Pair<b2BodyId, Entity>>()
//      val queryCallback: (Fixture) -> Boolean = {
//        val body = it.body
//        (body.userData as? Entity)?.also { entity -> entities += body to entity }
//        true
//      }
//      box2dWorld.QueryAABB(queryCallback, lowerX.toFloat(), lowerY.toFloat(), upperX.toFloat(), upperY.toFloat()) // blocking
      callback(entities)
    }
  }

  fun getPosition(bodyId: b2BodyId): Compacted2Float = Box2d.b2Body_GetPosition(bodyId).compactToFloat()

  fun getBlockPosition(bodyId: b2BodyId): WorldCompactLoc = Box2d.b2Body_GetPosition(bodyId).compactToInt()

  override val isDisposed: Boolean get() = disposed

  override fun dispose() {
    synchronized(BOX2D_LOCK) {
      if (disposed) {
        return
      }
      disposed = true
      Box2d.b2DestroyWorld(box2dWorld)
    }
  }

  companion object {
    const val X_WORLD_GRAVITY = 0f
    const val Y_WORLD_GRAVITY = 2f * -9.8f
  }
}
