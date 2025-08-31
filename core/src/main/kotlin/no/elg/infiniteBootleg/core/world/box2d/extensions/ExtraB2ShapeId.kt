@file:Suppress("unused")

package no.elg.infiniteBootleg.core.world.box2d.extensions

import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.enums.b2ShapeType
import com.badlogic.gdx.box2d.structs.b2AABB
import com.badlogic.gdx.box2d.structs.b2BodyId
import com.badlogic.gdx.box2d.structs.b2Capsule
import com.badlogic.gdx.box2d.structs.b2CastOutput
import com.badlogic.gdx.box2d.structs.b2ChainId
import com.badlogic.gdx.box2d.structs.b2ChainSegment
import com.badlogic.gdx.box2d.structs.b2Circle
import com.badlogic.gdx.box2d.structs.b2ContactData
import com.badlogic.gdx.box2d.structs.b2Filter
import com.badlogic.gdx.box2d.structs.b2MassData
import com.badlogic.gdx.box2d.structs.b2Polygon
import com.badlogic.gdx.box2d.structs.b2RayCastInput
import com.badlogic.gdx.box2d.structs.b2Segment
import com.badlogic.gdx.box2d.structs.b2ShapeId
import com.badlogic.gdx.box2d.structs.b2SurfaceMaterial
import com.badlogic.gdx.box2d.structs.b2Vec2
import com.badlogic.gdx.box2d.structs.b2WorldId
import com.badlogic.gdx.jnigen.runtime.pointer.VoidPointer
import no.elg.infiniteBootleg.core.Settings.handleInvalidBox2dRef
import no.elg.infiniteBootleg.core.world.box2d.VoidPointerManager
import no.elg.infiniteBootleg.core.world.box2d.genericSetUserData

val b2ShapeId.isValid: Boolean
  get() = Box2d.b2Shape_IsValid(this)

val b2ShapeId.body: b2BodyId
  get() = Box2d.b2Shape_GetBody(this)

val b2ShapeId.isSensor: Boolean
  get() = Box2d.b2Shape_IsSensor(this)

val b2ShapeId.type: b2ShapeType
  get() = Box2d.b2Shape_GetType(this)

val b2ShapeId.world: b2WorldId
  get() = Box2d.b2Shape_GetWorld(this)

val b2ShapeId.parentChain: b2ChainId
  get() = Box2d.b2Shape_GetParentChain(this)


var b2ShapeId.filter: b2Filter
  get() = Box2d.b2Shape_GetFilter(this)
  set(value) = Box2d.b2Shape_SetFilter(this, value)

var b2ShapeId.friction: Float
  get() = Box2d.b2Shape_GetFriction(this)
  set(value) = Box2d.b2Shape_SetFriction(this, value)

var b2ShapeId.restitution: Float
  get() = Box2d.b2Shape_GetRestitution(this)
  set(value) = Box2d.b2Shape_SetRestitution(this, value)

var b2ShapeId.material: Int
  get() = Box2d.b2Shape_GetMaterial(this)
  set(value) = Box2d.b2Shape_SetMaterial(this, value)

var b2ShapeId.surfaceMaterial: b2SurfaceMaterial
  get() = Box2d.b2Shape_GetSurfaceMaterial(this)
  set(value) = Box2d.b2Shape_SetSurfaceMaterial(this, value)


var b2ShapeId.density: Float
  get() = Box2d.b2Shape_GetDensity(this)
  set(value) = setDensity(value, true)

fun b2ShapeId.setDensity(value: Float, updateBodyMass: Boolean) =
  Box2d.b2Shape_SetDensity(this, value, updateBodyMass)

val b2ShapeId.contactCapacity: Int
  get() = Box2d.b2Shape_GetContactCapacity(this)

val b2ShapeId.sensorCapacity: Int
  get() = Box2d.b2Shape_GetSensorCapacity(this)

val b2ShapeId.aabb: b2AABB
  get() = Box2d.b2Shape_GetAABB(this)

val b2ShapeId.massData: b2MassData
  get() = Box2d.b2Shape_GetMassData(this)


val b2ShapeId.circle: b2Circle
  get() = Box2d.b2Shape_GetCircle(this)

val b2ShapeId.segment: b2Segment
  get() = Box2d.b2Shape_GetSegment(this)

val b2ShapeId.chainSegment: b2ChainSegment
  get() = Box2d.b2Shape_GetChainSegment(this)
val b2ShapeId.capsule: b2Capsule
  get() = Box2d.b2Shape_GetCapsule(this)

val b2ShapeId.polygon: b2Polygon
  get() = Box2d.b2Shape_GetPolygon(this)

fun b2ShapeId.setCircle(circle: b2Circle.b2CirclePointer) =
  Box2d.b2Shape_SetCircle(this, circle)

fun b2ShapeId.setCapsule(capsule: b2Capsule.b2CapsulePointer) =
  Box2d.b2Shape_SetCapsule(this, capsule)

fun b2ShapeId.setSegment(segment: b2Segment.b2SegmentPointer) =
  Box2d.b2Shape_SetSegment(this, segment)

fun b2ShapeId.setPolygon(polygon: b2Polygon.b2PolygonPointer) =
  Box2d.b2Shape_SetPolygon(this, polygon)


var b2ShapeId.sensorEventsEnabled: Boolean
  get() = Box2d.b2Shape_AreSensorEventsEnabled(this)
  set(value) = Box2d.b2Shape_EnableSensorEvents(this, value)

var b2ShapeId.contactEventsEnabled: Boolean
  get() = Box2d.b2Shape_AreContactEventsEnabled(this)
  set(value) = Box2d.b2Shape_EnableContactEvents(this, value)

var b2ShapeId.preSolveEventsEnabled: Boolean
  get() = Box2d.b2Shape_ArePreSolveEventsEnabled(this)
  set(value) = Box2d.b2Shape_EnablePreSolveEvents(this, value)

var b2ShapeId.hitEventsEnabled: Boolean
  get() = Box2d.b2Shape_AreHitEventsEnabled(this)
  set(value) = Box2d.b2Shape_EnableHitEvents(this, value)


fun b2ShapeId.getContactData(buffer: b2ContactData.b2ContactDataPointer, capacity: Int): Int =
  Box2d.b2Shape_GetContactData(this, buffer, capacity)

fun b2ShapeId.getSensorOverlaps(buffer: b2ShapeId.b2ShapeIdPointer, capacity: Int): Int =
  Box2d.b2Shape_GetSensorOverlaps(this, buffer, capacity)

fun b2ShapeId.closestPoint(target: b2Vec2): b2Vec2 =
  Box2d.b2Shape_GetClosestPoint(this, target)

fun b2ShapeId.rayCast(input: b2RayCastInput.b2RayCastInputPointer): b2CastOutput =
  Box2d.b2Shape_RayCast(this, input)

fun b2ShapeId.testPoint(point: b2Vec2): Boolean =
  Box2d.b2Shape_TestPoint(this, point)

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
