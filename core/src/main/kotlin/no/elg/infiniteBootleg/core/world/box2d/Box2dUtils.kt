package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.structs.b2BodyDef
import com.badlogic.gdx.box2d.structs.b2BodyEvents
import com.badlogic.gdx.box2d.structs.b2BodyId
import com.badlogic.gdx.box2d.structs.b2Capsule
import com.badlogic.gdx.box2d.structs.b2Circle
import com.badlogic.gdx.box2d.structs.b2ContactEvents
import com.badlogic.gdx.box2d.structs.b2Filter
import com.badlogic.gdx.box2d.structs.b2Polygon
import com.badlogic.gdx.box2d.structs.b2Rot
import com.badlogic.gdx.box2d.structs.b2SensorEvents
import com.badlogic.gdx.box2d.structs.b2ShapeDef
import com.badlogic.gdx.box2d.structs.b2ShapeId
import com.badlogic.gdx.box2d.structs.b2Vec2
import com.badlogic.gdx.box2d.structs.b2WorldId
import com.badlogic.gdx.jnigen.runtime.pointer.VoidPointer
import no.elg.infiniteBootleg.core.Settings.handleInvalidBox2dRef
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.util.Compacted2Float
import no.elg.infiniteBootleg.core.util.Compacted2Int
import no.elg.infiniteBootleg.core.util.Degrees
import no.elg.infiniteBootleg.core.util.Radians
import no.elg.infiniteBootleg.core.util.compactFloat
import no.elg.infiniteBootleg.core.util.compactInt
import no.elg.infiniteBootleg.core.util.decompactLocXf
import no.elg.infiniteBootleg.core.util.decompactLocYf
import no.elg.infiniteBootleg.core.util.toDegrees
import kotlin.reflect.KMutableProperty0

val NO_ROTATION: b2Rot = Box2d.b2MakeRot(0f)

fun makeB2Vec2(x: Number, y: Number): b2Vec2 = makeB2Vec2(x.toFloat(), y.toFloat())

fun makeB2Vec2(x: Float, y: Float): b2Vec2 =
  b2Vec2().apply {
    this.x(x)
    this.y(y)
  }

fun b2Vec2.set(x: Float, y: Float): b2Vec2 =
  apply {
    x(x)
    y(y)
  }

fun b2Vec2.compactToFloat(): Compacted2Float = compactFloat(x(), y())

fun b2Vec2.compactToInt(): Compacted2Int = compactInt(x().toInt(), y().toInt())

operator fun b2Vec2.component1(): Float = x()
operator fun b2Vec2.component2(): Float = y()

inline val b2Vec2.x get() = x()
inline val b2Vec2.y get() = y()

fun Compacted2Float.tob2Vec2(): b2Vec2 = b2Vec2().set(x = this.decompactLocXf(), y = this.decompactLocYf())

// /////////////
//  b2BodyId //
// /////////////

var b2BodyId.isAwake: Boolean
  get() = Box2d.b2Body_IsAwake(this)
  set(value) {
    Box2d.b2Body_SetAwake(this, value)
  }

var b2BodyId.gravityScale: Float
  get() = Box2d.b2Body_GetGravityScale(this)
  set(value) {
    Box2d.b2Body_SetGravityScale(this, value)
  }

var b2BodyId.position: b2Vec2
  get() = Box2d.b2Body_GetPosition(this)
  set(value) {
    Box2d.b2Body_SetTransform(this, value, NO_ROTATION)
  }

var b2BodyId.velocity: b2Vec2
  get() = Box2d.b2Body_GetLinearVelocity(this)
  set(value) {
    Box2d.b2Body_SetLinearVelocity(this, value)
  }

val b2BodyId.rotation: b2Rot get() = Box2d.b2Body_GetRotation(this)

val b2BodyId.mass: Float
  get() = Box2d.b2Body_GetMass(this)

val b2BodyId.world: b2WorldId get() = Box2d.b2Body_GetWorld(this)
// val b2BodyId.name: String get() = Box2d.b2Body_GetName(this)

val b2BodyId.isValid: Boolean get() = Box2d.b2Body_IsValid(this)

val b2BodyId.isEnabled: Boolean get() = Box2d.b2Body_IsEnabled(this)

fun b2BodyId.applyForceToCenter(force: b2Vec2, wake: Boolean) {
  Box2d.b2Body_ApplyForceToCenter(this, force, wake)
}

val b2BodyId.shapes: List<b2ShapeId>
  get() {
    val shapeCount = Box2d.b2Body_GetShapeCount(this)
    if (shapeCount == 0) return emptyList()
    val b2ShapeIdPointer = b2ShapeId.b2ShapeIdPointer(shapeCount, true)
    val stored = Box2d.b2Body_GetShapes(this, b2ShapeIdPointer, shapeCount)
    require(shapeCount == stored)
    return (0..<stored).map(b2ShapeIdPointer::get)
  }

fun b2BodyId.dispose() {
  userData = null
  Box2d.b2DestroyBody(this)
}

fun b2BodyId.createPolygonShape(shapeDef: b2ShapeDef, polygon: b2Polygon, userData: Any? = null): b2ShapeId =
  Box2d.b2CreatePolygonShape(this, shapeDef.asPointer(), polygon.asPointer()).also { shape -> userData?.also { shape.userData = it } }

fun b2BodyId.createCircleShape(shapeDef: b2ShapeDef, circle: b2Circle, userData: Any? = null): b2ShapeId =
  Box2d.b2CreateCircleShape(this, shapeDef.asPointer(), circle.asPointer()).also { shape -> userData?.also { shape.userData = it } }

fun b2BodyId.createCapsuleShape(shapeDef: b2ShapeDef, capsule: b2Capsule, userData: Any? = null): b2ShapeId =
  Box2d.b2CreateCapsuleShape(this, shapeDef.asPointer(), capsule.asPointer()).also { shape -> userData?.also { shape.userData = it } }

var b2BodyId.userDataPointer: VoidPointer
  get() = if (Box2d.b2Body_IsValid(this)) {
    Box2d.b2Body_GetUserData(this)
  } else {
    handleInvalidBox2dRef.handle { "Tried to get user data from invalid body $this" }
    VoidPointer.NULL
  }
  set(value) {
    if (Box2d.b2Body_IsValid(this)) {
      Box2d.b2Body_SetUserData(this, value)
    } else {
      handleInvalidBox2dRef.handle { "Tried to set user data on invalid body $this" }
    }
  }

var b2BodyId.userData: Any?
  get() = VoidPointerManager.deferenceVoidPointer(userDataPointer)
  set(value) {
    genericSetUserData(value, this::userDataPointer)
  }

// /////////////
// b2WorldId //
// /////////////

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

var b2WorldId.userData: Any?
  get() = VoidPointerManager.deferenceVoidPointer(userDataPointer)
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

fun b2WorldId.step(timeStep: Float, subStepCount: Int) {
  Box2d.b2World_Step(this, timeStep, subStepCount)
}

fun b2WorldId.getBodyEvents(): b2BodyEvents = Box2d.b2World_GetBodyEvents(this)
fun b2WorldId.getSensorEvents(): b2SensorEvents = Box2d.b2World_GetSensorEvents(this)
fun b2WorldId.getContactEvents(): b2ContactEvents = Box2d.b2World_GetContactEvents(this)

fun b2WorldId.createBody(bodyDef: b2BodyDef): b2BodyId = Box2d.b2CreateBody(this, bodyDef.asPointer())

fun b2WorldId.dispose() {
  userData = null
  Box2d.b2DestroyWorld(this)
  VoidPointerManager.globalVPM.clean()
}

// /////////////
//   b2Rot   //
// /////////////

val b2Rot.radians: Radians get() = Box2d.b2Rot_GetAngle(this)
val b2Rot.degrees: Degrees get() = radians.toDegrees()

// /////////////
// b2ShapeId //
// /////////////

var b2ShapeId.filter: b2Filter
  get() = Box2d.b2Shape_GetFilter(this)
  set(value) {
    Box2d.b2Shape_SetFilter(this, value)
  }

fun b2ShapeId.dispose(updateBodyMass: Boolean = true) {
  userData = null
  Box2d.b2DestroyShape(this, updateBodyMass)
}

/**
 * Interact directly with the user data pointer of a shape.
 *
 * This might lead to memory leaks if the pointer is not removed from [VoidPointerManager]
 */
private var b2ShapeId.userDataPointer: VoidPointer
  get() = if (Box2d.b2Shape_IsValid(this)) {
    Box2d.b2Shape_GetUserData(this)
  } else {
    handleInvalidBox2dRef.handle { "Tried to get user data from invalid shape $this" }
    VoidPointer.NULL
  }
  set(value) {
    if (Box2d.b2Shape_IsValid(this)) {
      Box2d.b2Shape_SetUserData(this, value)
    } else {
      handleInvalidBox2dRef.handle { "Tried to set user data on invalid shape $this" }
    }
  }

var b2ShapeId.userData: Any?
  get() = VoidPointerManager.deferenceVoidPointer(userDataPointer)
  set(value) {
    genericSetUserData(value, this::userDataPointer)
  }

/**
 * Correctly set the user data pointer of a shape. Handles `null` values by removing the pointer from the [VoidPointerManager].
 */
private fun genericSetUserData(value: Any?, property: KMutableProperty0<VoidPointer>) {
  ThreadType.PHYSICS.launchOrRun {
    if (value == null) {
      val pointer = property.get()
      VoidPointerManager.removePointer(pointer)
      property.set(VoidPointer.NULL)
    } else {
      property.set(VoidPointerManager.createVoidPointer(value))
    }
  }
}

/**
 * @return Whether the pointer cannot be used
 */
val VoidPointer.isInvalid: Boolean get() = isFreed || isNull

/**
 * @return Whether the pointer can be used
 */
val VoidPointer.isValid: Boolean get() = !isInvalid
