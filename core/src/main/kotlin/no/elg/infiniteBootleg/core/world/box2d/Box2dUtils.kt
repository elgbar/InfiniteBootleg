package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.structs.b2BodyId
import com.badlogic.gdx.box2d.structs.b2Rot
import com.badlogic.gdx.box2d.structs.b2Vec2
import com.badlogic.gdx.box2d.structs.b2WorldId
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

val b2WorldId.gravity: b2Vec2 get() = Box2d.b2World_GetGravity(this)

val b2Rot.radians: Radians get() = Box2d.b2Rot_GetAngle(this)
val b2Rot.degrees: Degrees get() = radians.toDegrees()
