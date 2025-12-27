@file:Suppress("unused")

package no.elg.infiniteBootleg.core.world.box2d.extensions

import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.structs.b2AABB
import com.badlogic.gdx.box2d.structs.b2BodyDef
import com.badlogic.gdx.box2d.structs.b2BodyEvents
import com.badlogic.gdx.box2d.structs.b2BodyId
import com.badlogic.gdx.box2d.structs.b2Capsule
import com.badlogic.gdx.box2d.structs.b2ContactEvents
import com.badlogic.gdx.box2d.structs.b2Counters
import com.badlogic.gdx.box2d.structs.b2ExplosionDef
import com.badlogic.gdx.box2d.structs.b2Profile
import com.badlogic.gdx.box2d.structs.b2QueryFilter
import com.badlogic.gdx.box2d.structs.b2RayResult
import com.badlogic.gdx.box2d.structs.b2SensorEvents
import com.badlogic.gdx.box2d.structs.b2ShapeId
import com.badlogic.gdx.box2d.structs.b2Vec2
import com.badlogic.gdx.box2d.structs.b2WorldId
import com.badlogic.gdx.jnigen.runtime.closure.ClosureObject
import com.badlogic.gdx.jnigen.runtime.pointer.VoidPointer
import no.elg.infiniteBootleg.core.Settings.handleInvalidBox2dRef
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.exceptions.CalledFromWrongThreadTypeException
import no.elg.infiniteBootleg.core.world.box2d.ALLOW_ALL_QUERY_FILTER
import no.elg.infiniteBootleg.core.world.box2d.VoidPointerManager
import no.elg.infiniteBootleg.core.world.box2d.VoidPointerManager.Companion.deferenceVoidPointer
import no.elg.infiniteBootleg.core.world.box2d.genericSetUserData
import no.elg.infiniteBootleg.core.world.box2d.use
import no.elg.infiniteBootleg.core.world.world.World

var b2WorldId.sleepingEnabled: Boolean
  get() = Box2d.b2World_IsSleepingEnabled(this)
  set(value) = Box2d.b2World_EnableSleeping(this, value)

var b2WorldId.continuousEnabled: Boolean
  get() = Box2d.b2World_IsContinuousEnabled(this)
  set(value) = Box2d.b2World_EnableContinuous(this, value)

var b2WorldId.warmStartingEnabled: Boolean
  get() = Box2d.b2World_IsWarmStartingEnabled(this)
  set(value) = Box2d.b2World_EnableWarmStarting(this, value)

var b2WorldId.hitEventThreshold: Float
  get() = Box2d.b2World_GetHitEventThreshold(this)
  set(value) = Box2d.b2World_SetHitEventThreshold(this, value)

fun b2WorldId.setContactTuning(hertz: Float, dampingRatio: Float, pushSpeed: Float) {
  Box2d.b2World_SetContactTuning(this, hertz, dampingRatio, pushSpeed)
}

fun b2WorldId.dumpMemoryStats() = Box2d.b2World_DumpMemoryStats(this)

val b2WorldId.awakeBodyCount: Int
  get() = Box2d.b2World_GetAwakeBodyCount(this)

fun b2WorldId.getProfile(): b2Profile = Box2d.b2World_GetProfile(this)
fun b2WorldId.getCounters(): b2Counters = Box2d.b2World_GetCounters(this)

fun b2WorldId.explode(explosionDef: b2ExplosionDef.b2ExplosionDefPointer) {
  Box2d.b2World_Explode(this, explosionDef)
}

fun b2WorldId.castRayClosest(origin: b2Vec2, translation: b2Vec2, filter: b2QueryFilter): b2RayResult = Box2d.b2World_CastRayClosest(this, origin, translation, filter)

fun b2WorldId.castMover(mover: b2Capsule.b2CapsulePointer, translation: b2Vec2, filter: b2QueryFilter): Float = Box2d.b2World_CastMover(this, mover, translation, filter)

var b2WorldId.gravity: b2Vec2
  get() = Box2d.b2World_GetGravity(this)
  set(value) = Box2d.b2World_SetGravity(this, value)

var b2WorldId.userDataPointer: VoidPointer
  get() = if (Box2d.b2World_IsValid(this)) {
    Box2d.b2World_GetUserData(this)
  } else {
    handleInvalidBox2dRef.handle { "Tried to get user data from invalid world $this" }
    VoidPointer.NULL
  }
  set(value) {
    if (Box2d.b2World_IsValid(this)) {
      Box2d.b2World_SetUserData(this, value)
    } else {
      handleInvalidBox2dRef.handle { "Tried to set user data on invalid world $this" }
    }
  }

/**
 * The world associated with this box2d world
 *
 * @throws CalledFromWrongThreadTypeException if the thread is not [ThreadType.PHYSICS]
 */
var b2WorldId.userData: World?
  get() = deferenceVoidPointer(userDataPointer) as World
  set(value) {
    genericSetUserData(value, this::userDataPointer)
  }

var b2WorldId.restitutionThreshold: Float
  get() = Box2d.b2World_GetRestitutionThreshold(this)
  set(value) = Box2d.b2World_SetRestitutionThreshold(this, value)

var b2WorldId.maximumLinearSpeed: Float
  get() = Box2d.b2World_GetMaximumLinearSpeed(this)
  set(value) = Box2d.b2World_SetMaximumLinearSpeed(this, value)

val b2WorldId.isValid: Boolean get() = Box2d.b2World_IsValid(this)

// //////////////
//  Functions  //
// //////////////

fun b2WorldId.step(timeStep: Float, subStepCount: Int) {
  Box2d.b2World_Step(this, timeStep, subStepCount)
}

fun b2WorldId.getBodyEvents(): b2BodyEvents = Box2d.b2World_GetBodyEvents(this)
fun b2WorldId.getSensorEvents(): b2SensorEvents = Box2d.b2World_GetSensorEvents(this)
fun b2WorldId.getContactEvents(): b2ContactEvents = Box2d.b2World_GetContactEvents(this)

fun b2WorldId.createBody(bodyDef: b2BodyDef): b2BodyId = Box2d.b2CreateBody(this, bodyDef.asPointer())

fun b2WorldId.overlapAABB(aabb: b2AABB, filter: b2QueryFilter = ALLOW_ALL_QUERY_FILTER, callback: (shapeId: b2ShapeId, context: VoidPointer) -> Boolean) {
  ClosureObject.fromClosure(Box2d.b2OverlapResultFcn(callback)).use { callbackClosure ->
    Box2d.b2World_OverlapAABB(this, aabb, filter, callbackClosure, VoidPointer.NULL)
  }
}

fun b2WorldId.dispose() {
  userData = null
  Box2d.b2DestroyWorld(this)
  VoidPointerManager.globalVPM.clear()
}
