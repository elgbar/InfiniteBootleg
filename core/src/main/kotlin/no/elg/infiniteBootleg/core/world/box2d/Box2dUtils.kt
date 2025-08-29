package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.structs.b2Rot
import com.badlogic.gdx.jnigen.runtime.closure.Closure
import com.badlogic.gdx.jnigen.runtime.closure.ClosureObject
import com.badlogic.gdx.jnigen.runtime.pointer.VoidPointer
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.util.Degrees
import no.elg.infiniteBootleg.core.util.Radians
import no.elg.infiniteBootleg.core.util.toDegrees
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KMutableProperty0

// ////////////
//   b2Rot   //
// ////////////

val NO_ROTATION: b2Rot = Box2d.b2MakeRot(0f)

val b2Rot.radians: Radians get() = Box2d.b2Rot_GetAngle(this)
val b2Rot.degrees: Degrees get() = radians.toDegrees()

// //////////////////
//   VoidPointer   //
// //////////////////

/**
 * @return Whether the pointer cannot be used
 */
val VoidPointer.isInvalid: Boolean get() = isFreed || isNull

/**
 * @return Whether the pointer can be used
 */
val VoidPointer.isValid: Boolean get() = !isInvalid

// /////////////
//   helpers   //
// /////////////

/**
 * Correctly set the user data pointer of a shape. Handles `null` values by removing the pointer from the [VoidPointerManager].
 */
internal fun genericSetUserData(value: Any?, property: KMutableProperty0<VoidPointer>) {
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

inline fun <T : Closure, R> ClosureObject<T>.use(action: (ClosureObject<T>) -> R): R {
  contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
  return try {
    action(this)
  } finally {
    free()
  }
}
