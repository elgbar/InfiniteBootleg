@file:Suppress("unused")

package no.elg.infiniteBootleg.core.world.box2d.extensions

import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.enums.b2BodyType
import com.badlogic.gdx.box2d.structs.b2BodyId
import com.badlogic.gdx.box2d.structs.b2Capsule
import com.badlogic.gdx.box2d.structs.b2ChainDef
import com.badlogic.gdx.box2d.structs.b2ChainId
import com.badlogic.gdx.box2d.structs.b2Circle
import com.badlogic.gdx.box2d.structs.b2MassData
import com.badlogic.gdx.box2d.structs.b2Polygon
import com.badlogic.gdx.box2d.structs.b2Rot
import com.badlogic.gdx.box2d.structs.b2Segment
import com.badlogic.gdx.box2d.structs.b2ShapeDef
import com.badlogic.gdx.box2d.structs.b2ShapeId
import com.badlogic.gdx.box2d.structs.b2Transform
import com.badlogic.gdx.box2d.structs.b2Vec2
import com.badlogic.gdx.box2d.structs.b2WorldId
import com.badlogic.gdx.jnigen.runtime.pointer.VoidPointer
import no.elg.infiniteBootleg.core.Settings.handleInvalidBox2dRef

// ////////////////////
// Simple properties //
// ////////////////////

var b2BodyId.type: b2BodyType
  get() = Box2d.b2Body_GetType(this)
  set(value) {
    Box2d.b2Body_SetType(this, value)
  }

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
    Box2d.b2Body_SetTransform(this, value, no.elg.infiniteBootleg.core.world.box2d.NO_ROTATION)
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

val b2BodyId.isValid: Boolean get() = Box2d.b2Body_IsValid(this)

var b2BodyId.isEnabled: Boolean
  get() = Box2d.b2Body_IsEnabled(this)
  set(enable) = if (enable) Box2d.b2Body_Enable(this) else Box2d.b2Body_Disable(this)

var b2BodyId.linearDamping: Float
  get() = Box2d.b2Body_GetLinearDamping(this)
  set(value) = Box2d.b2Body_SetLinearDamping(this, value)

var b2BodyId.angularDamping: Float
  get() = Box2d.b2Body_GetAngularDamping(this)
  set(value) = Box2d.b2Body_SetAngularDamping(this, value)

val b2BodyId.localCenterOfMass: b2Vec2
  get() = Box2d.b2Body_GetLocalCenterOfMass(this)

val b2BodyId.worldCenterOfMass: b2Vec2
  get() = Box2d.b2Body_GetWorldCenterOfMass(this)

var b2BodyId.angularVelocity: Float
  get() = Box2d.b2Body_GetAngularVelocity(this)
  set(value) = Box2d.b2Body_SetAngularVelocity(this, value)

// //////////////////////
// Advanced properties //
// //////////////////////

// val b2BodyId.name: String get() = Box2d.b2Body_GetName(this)

val b2BodyId.shapes: List<b2ShapeId>
  get() {
    val shapeCount = Box2d.b2Body_GetShapeCount(this)
    if (shapeCount == 0) return emptyList()
    val b2ShapeIdPointer = b2ShapeId.b2ShapeIdPointer(shapeCount, true)
    val stored = Box2d.b2Body_GetShapes(this, b2ShapeIdPointer, shapeCount)
    require(shapeCount == stored)
    return (0..<stored).map(b2ShapeIdPointer::get)
  }

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
  get() = no.elg.infiniteBootleg.core.world.box2d.VoidPointerManager.deferenceVoidPointer(userDataPointer)
  set(value) {
    no.elg.infiniteBootleg.core.world.box2d.genericSetUserData(value, this::userDataPointer)
  }

/**
 * Note: position can be set directly with [b2BodyId.position]
 */
fun b2BodyId.setTransform(position: b2Vec2, angle: b2Rot) {
  Box2d.b2Body_SetTransform(this, position, angle)
}

val b2BodyId.transform: b2Transform
  get() = Box2d.b2Body_GetTransform(this)

var b2BodyId.massData: b2MassData
  get() = Box2d.b2Body_GetMassData(this)
  set(value) = Box2d.b2Body_SetMassData(this, value)

var b2BodyId.fixedRotation: Boolean
  get() = Box2d.b2Body_IsFixedRotation(this)
  set(value) = Box2d.b2Body_SetFixedRotation(this, value)

var b2BodyId.isBullet: Boolean
  get() = Box2d.b2Body_IsBullet(this)
  set(value) = Box2d.b2Body_SetBullet(this, value)

// //////////////
//  Functions  //
// //////////////

fun b2BodyId.applyForce(force: b2Vec2, point: b2Vec2, wake: Boolean) {
  Box2d.b2Body_ApplyForce(this, force, point, wake)
}

fun b2BodyId.applyTorque(torque: Float, wake: Boolean) {
  Box2d.b2Body_ApplyTorque(this, torque, wake)
}

fun b2BodyId.applyLinearImpulse(impulse: b2Vec2, point: b2Vec2, wake: Boolean) {
  Box2d.b2Body_ApplyLinearImpulse(this, impulse, point, wake)
}

fun b2BodyId.applyLinearImpulseToCenter(impulse: b2Vec2, wake: Boolean) {
  Box2d.b2Body_ApplyLinearImpulseToCenter(this, impulse, wake)
}

fun b2BodyId.applyAngularImpulse(impulse: Float, wake: Boolean) {
  Box2d.b2Body_ApplyAngularImpulse(this, impulse, wake)
}

fun b2BodyId.applyForceToCenter(force: b2Vec2, wake: Boolean) {
  Box2d.b2Body_ApplyForceToCenter(this, force, wake)
}

fun b2BodyId.createPolygonShape(shapeDef: b2ShapeDef, polygon: b2Polygon, userData: Any? = null): b2ShapeId =
  Box2d.b2CreatePolygonShape(this, shapeDef.asPointer(), polygon.asPointer()).also { shape -> userData?.also { shape.userData = it } }

fun b2BodyId.createCircleShape(shapeDef: b2ShapeDef, circle: b2Circle, userData: Any? = null): b2ShapeId =
  Box2d.b2CreateCircleShape(this, shapeDef.asPointer(), circle.asPointer()).also { shape -> userData?.also { shape.userData = it } }

fun b2BodyId.createCapsuleShape(shapeDef: b2ShapeDef, capsule: b2Capsule, userData: Any? = null): b2ShapeId =
  Box2d.b2CreateCapsuleShape(this, shapeDef.asPointer(), capsule.asPointer()).also { shape -> userData?.also { shape.userData = it } }

fun b2BodyId.createSegmentShape(shapeDef: b2ShapeDef.b2ShapeDefPointer, segment: b2Segment.b2SegmentPointer, userData: Any? = null): b2ShapeId =
  Box2d.b2CreateSegmentShape(this, shapeDef, segment).also { it.userData = userData }

fun b2BodyId.createChainShape(chainDef: b2ChainDef.b2ChainDefPointer): b2ChainId = Box2d.b2CreateChain(this, chainDef)

fun b2BodyId.dispose() {
  userData = null
  Box2d.b2DestroyBody(this)
}
