package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.structs.b2BodyDef
import com.badlogic.gdx.box2d.structs.b2BodyEvents
import com.badlogic.gdx.box2d.structs.b2BodyId
import com.badlogic.gdx.box2d.structs.b2Capsule
import com.badlogic.gdx.box2d.structs.b2Circle
import com.badlogic.gdx.box2d.structs.b2Filter
import com.badlogic.gdx.box2d.structs.b2Polygon
import com.badlogic.gdx.box2d.structs.b2Rot
import com.badlogic.gdx.box2d.structs.b2ShapeDef
import com.badlogic.gdx.box2d.structs.b2ShapeId
import com.badlogic.gdx.box2d.structs.b2Vec2
import com.badlogic.gdx.box2d.structs.b2WorldId
import com.badlogic.gdx.jnigen.runtime.pointer.VoidPointer
import no.elg.infiniteBootleg.core.util.Compacted2Float
import no.elg.infiniteBootleg.core.util.Compacted2Int
import no.elg.infiniteBootleg.core.util.Degrees
import no.elg.infiniteBootleg.core.util.Radians
import no.elg.infiniteBootleg.core.util.compactFloat
import no.elg.infiniteBootleg.core.util.compactInt
import no.elg.infiniteBootleg.core.util.decompactLocXf
import no.elg.infiniteBootleg.core.util.decompactLocYf
import no.elg.infiniteBootleg.core.util.toDegrees

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


///////////////
//  b2BodyId //
///////////////

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

fun b2BodyId.destroy() {
  Box2d.b2DestroyBody(this)
}

fun b2BodyId.createPolygonShape(shapeDef: b2ShapeDef, polygon: b2Polygon): b2ShapeId {
  return Box2d.b2CreatePolygonShape(this, shapeDef.asPointer(), polygon.asPointer())
}

fun b2BodyId.createCircleShape(shapeDef: b2ShapeDef, circle: b2Circle): b2ShapeId {
  return Box2d.b2CreateCircleShape(this, shapeDef.asPointer(), circle.asPointer())
}

fun b2BodyId.createCapsuleShape(shapeDef: b2ShapeDef, capsule: b2Capsule): b2ShapeId {
  return Box2d.b2CreateCapsuleShape(this, shapeDef.asPointer(), capsule.asPointer())
}

///////////////
// b2WorldId //
///////////////

var b2WorldId.gravity: b2Vec2
  get() = Box2d.b2World_GetGravity(this)
  set(value) = Box2d.b2World_SetGravity(this, value)

var b2WorldId.userData: VoidPointer
  get() = Box2d.b2World_GetUserData(this)
  set(value) = Box2d.b2World_SetUserData(this, value)

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

fun b2WorldId.createBody(bodyDef: b2BodyDef): b2BodyId = Box2d.b2CreateBody(this, bodyDef.asPointer())

fun b2WorldId.dispose() {
  Box2d.b2DestroyWorld(this)
}


///////////////
//   b2Rot   //
///////////////

val b2Rot.radians: Radians get() = Box2d.b2Rot_GetAngle(this)
val b2Rot.degrees: Degrees get() = radians.toDegrees()

///////////////
// b2ShapeId //
///////////////

var b2ShapeId.filter: b2Filter
  get() = Box2d.b2Shape_GetFilter(this)
  set(value) {
    Box2d.b2Shape_SetFilter(this, value)
  }

var b2ShapeId.userData: VoidPointer
  get() = Box2d.b2Shape_GetUserData(this)
  set(value) {
    Box2d.b2Shape_SetUserData(this, value)
  }
