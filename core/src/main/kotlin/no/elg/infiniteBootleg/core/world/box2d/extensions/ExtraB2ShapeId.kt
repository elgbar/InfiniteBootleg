package no.elg.infiniteBootleg.core.world.box2d.extensions

import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.structs.b2Filter
import com.badlogic.gdx.box2d.structs.b2ShapeId
import com.badlogic.gdx.jnigen.runtime.pointer.VoidPointer
import no.elg.infiniteBootleg.core.Settings.handleInvalidBox2dRef

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
 * This might lead to memory leaks if the pointer is not removed from [no.elg.infiniteBootleg.core.world.box2d.VoidPointerManager]
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
  get() = _root_ide_package_.no.elg.infiniteBootleg.core.world.box2d.VoidPointerManager.Companion.deferenceVoidPointer(userDataPointer)
  set(value) {
    _root_ide_package_.no.elg.infiniteBootleg.core.world.box2d.genericSetUserData(value, this::userDataPointer)
  }
