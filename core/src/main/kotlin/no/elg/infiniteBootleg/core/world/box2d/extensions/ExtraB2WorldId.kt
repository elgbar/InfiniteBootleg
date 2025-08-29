package no.elg.infiniteBootleg.core.world.box2d.extensions

import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.structs.b2AABB
import com.badlogic.gdx.box2d.structs.b2BodyDef
import com.badlogic.gdx.box2d.structs.b2BodyEvents
import com.badlogic.gdx.box2d.structs.b2BodyId
import com.badlogic.gdx.box2d.structs.b2ContactEvents
import com.badlogic.gdx.box2d.structs.b2QueryFilter
import com.badlogic.gdx.box2d.structs.b2SensorEvents
import com.badlogic.gdx.box2d.structs.b2ShapeId
import com.badlogic.gdx.box2d.structs.b2Vec2
import com.badlogic.gdx.box2d.structs.b2WorldId
import com.badlogic.gdx.jnigen.runtime.closure.ClosureObject
import com.badlogic.gdx.jnigen.runtime.pointer.VoidPointer
import no.elg.infiniteBootleg.core.Settings.handleInvalidBox2dRef
import no.elg.infiniteBootleg.core.world.box2d.use

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
  get() = _root_ide_package_.no.elg.infiniteBootleg.core.world.box2d.VoidPointerManager.Companion.deferenceVoidPointer(userDataPointer)
  set(value) {
    _root_ide_package_.no.elg.infiniteBootleg.core.world.box2d.genericSetUserData(value, this::userDataPointer)
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

fun b2WorldId.overlapAABB(aabb: b2AABB, filter: b2QueryFilter, callback: (shapeId: b2ShapeId, context: VoidPointer) -> Boolean) {
  ClosureObject.fromClosure(Box2d.b2OverlapResultFcn(callback)).use { callbackClosure ->
    Box2d.b2World_OverlapAABB(this, aabb, filter, callbackClosure, VoidPointer.NULL)
  }
}

fun b2WorldId.dispose() {
  userData = null
  Box2d.b2DestroyWorld(this)
  _root_ide_package_.no.elg.infiniteBootleg.core.world.box2d.VoidPointerManager.Companion.globalVPM.clean()
}
