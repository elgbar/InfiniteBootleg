package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.structs.b2BodyDef
import com.badlogic.gdx.box2d.structs.b2BodyId
import com.badlogic.gdx.box2d.structs.b2ShapeId
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
import no.elg.infiniteBootleg.core.util.IllegalAction
import no.elg.infiniteBootleg.core.util.MAX_WORLD_VEL
import no.elg.infiniteBootleg.core.util.WorldCompactLoc
import no.elg.infiniteBootleg.core.util.isBeingRemoved
import no.elg.infiniteBootleg.core.world.BOX2D_LOCK
import no.elg.infiniteBootleg.core.world.box2d.extensions.compactToFloat
import no.elg.infiniteBootleg.core.world.box2d.extensions.compactToInt
import no.elg.infiniteBootleg.core.world.box2d.extensions.createBody
import no.elg.infiniteBootleg.core.world.box2d.extensions.dispose
import no.elg.infiniteBootleg.core.world.box2d.extensions.isEnabled
import no.elg.infiniteBootleg.core.world.box2d.extensions.isValid
import no.elg.infiniteBootleg.core.world.box2d.extensions.makeB2AABB
import no.elg.infiniteBootleg.core.world.box2d.extensions.makeB2Vec2
import no.elg.infiniteBootleg.core.world.box2d.extensions.overlapAABB
import no.elg.infiniteBootleg.core.world.box2d.extensions.shapes
import no.elg.infiniteBootleg.core.world.box2d.extensions.step
import no.elg.infiniteBootleg.core.world.box2d.extensions.userData
import no.elg.infiniteBootleg.core.world.box2d.extensions.userDataPointer
import no.elg.infiniteBootleg.core.world.ticker.PostRunnableHandler
import no.elg.infiniteBootleg.core.world.ticker.WorldBox2DTicker.Companion.BOX2D_SUB_STEP_COUNT
import no.elg.infiniteBootleg.core.world.ticker.WorldBox2DTicker.Companion.BOX2D_TIME_STEP
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason

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
      worldDef.gravity = makeB2Vec2(X_WORLD_GRAVITY, Y_WORLD_GRAVITY)
      worldDef.maximumLinearSpeed(MAX_WORLD_VEL)
      box2dWorld = Box2d.b2CreateWorld(worldDef.asPointer())

      if (!box2dWorld.isValid) {
        throw IllegalStateException("Box2D world is not valid")
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

  private val contactEventManager = ContactManager(box2dWorld, world.engine)

  /**
   * Posts a [Runnable] on the physics thread.
   * The runnable will be executed under the synchronization of [BOX2D_LOCK].
   *
   * @see [com.badlogic.gdx.Application.postRunnable]
   */
  fun postBox2dRunnable(runnable: () -> Unit) = postRunnable.postRunnable(runnable)

  /**
   * Thread safe and fastest way to remove an entity from the world
   */
  internal fun removeEntity(entity: Entity) {
    if (entity.isBeingRemoved) {
      return
    }
    entity.enableFlag(INVALID_FLAG)
    ThreadType.PHYSICS.launchOrRun {
      world.engine.removeEntity(entity)
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
    ThreadType.PHYSICS.launchOrRun {
      createBodyNow(def, callback)
    }
  }

  @GuardedBy("BOX2D_LOCK")
  fun createBodyNow(def: b2BodyDef, callback: (b2BodyId) -> Unit): b2BodyId {
    ThreadType.requireCorrectThreadType(ThreadType.PHYSICS)
    return box2dWorld.createBody(def).also { body ->
      callback(body)
      val userData = body.userData
      if (userData == null) {
        IllegalAction.STACKTRACE.handle { "Userdata not added when creating body. Tried to c${body.userDataPointer.isValid}" }
      }
    }
  }

  /**
   * Destroy the given body, this method can be called from any thread
   *
   * @param body The body to destroy
   */
  fun destroyBody(body: b2BodyId) {
    ThreadType.PHYSICS.launchOrRun {
      require(body.isValid) { "Cannot destroy body that is not valid" }
      if (!body.isEnabled) {
        logger.error { "Trying to destroy an disabled body, the program will probably crash, userData: ${body.userData}" }
      }

      body.shapes.mapNotNull(b2ShapeId::userData).forEach {
        if (it is Entity) {
          world.removeEntity(it, DespawnReason.CHUNK_UNLOADED)
        }
        VoidPointerManager.remove(it)
      }
      body.dispose()
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
        box2dWorld.step(BOX2D_TIME_STEP, BOX2D_SUB_STEP_COUNT)
        contactEventManager.postBox2dStepEvents()
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
    ThreadType.PHYSICS.launchOrRun {
      val entities = mutableSetOf<Pair<b2BodyId, Entity>>()
      val aabb = makeB2AABB(lowerX, lowerY, upperX, upperY)
      box2dWorld.overlapAABB(aabb) { shapeId, _ ->
        val body = Box2d.b2Shape_GetBody(shapeId)
        (body.userData as? Entity)?.also { entity -> entities += body to entity }
        true // continue query
      }
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
      box2dWorld.dispose()
    }
  }

  companion object {
    const val X_WORLD_GRAVITY = 0f
    const val Y_WORLD_GRAVITY = 2f * -9.8f
  }
}
