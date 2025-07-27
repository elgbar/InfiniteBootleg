package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.gdx.jnigen.runtime.pointer.VoidPointer
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import no.elg.infiniteBootleg.core.events.api.ThreadType

/**
 * Not thread-safe
 */
class VoidPointerManager {

  private var id: Long = 1 // Start at 1 to avoid 0 being a valid pointer, which is used for null pointers
  private val addrToObj = Long2ObjectOpenHashMap<Any>()
  private val objToAddr = Object2LongOpenHashMap<Any>().also { it.defaultReturnValue(NOT_IN_MANAGER) }

  private fun nextId(): Long = id++

  /**
   * Create a new pointer to the given object.
   * If the object is null, a null pointer will be returned
   */
  fun createPointer(obj: Any?): VoidPointer {
    if (obj == null) {
      return VoidPointer.NULL
    }
    ThreadType.requireCorrectThreadType(ThreadType.PHYSICS)
    val existingPointer = objToAddr.getLong(obj)
    if (existingPointer != NOT_IN_MANAGER) {
      return VoidPointer(existingPointer, false)
    } else {
      val id = nextId()
      objToAddr.put(obj, id)
      addrToObj.put(id, obj)
      return VoidPointer(id, false)
    }
  }

  private fun removePointer(pointer: VoidPointer): Boolean {
    if (pointer.isInvalid) {
      return false
    }
    ThreadType.requireCorrectThreadType(ThreadType.PHYSICS)
    val id = pointer.getPointer()
    val obj = addrToObj.remove(id) ?: return false
    val addr = objToAddr.removeLong(obj)
    return addr != NOT_IN_MANAGER
  }

  private fun removeObject(obj: Any): Boolean {
    ThreadType.requireCorrectThreadType(ThreadType.PHYSICS)
    val addr = objToAddr.removeLong(obj)
    return if (addr != NOT_IN_MANAGER) {
      addrToObj.remove(addr) != null
    } else {
      false
    }
  }

  fun remove(obj: Any?): Boolean =
    when (obj) {
      null -> false
      is VoidPointer -> removePointer(obj)
      else -> removeObject(obj)
    }

  fun deferencePointer(pointer: VoidPointer): Any? {
    if (pointer.isInvalid) {
      return null
    }
    val id = pointer.getPointer()
    return addrToObj.get(id)
  }

  companion object {
    private const val NOT_IN_MANAGER = Long.MIN_VALUE

    // TODO make a vpm per world
    val globalVPM: VoidPointerManager = VoidPointerManager()

    fun createVoidPointer(obj: Any?): VoidPointer = globalVPM.createPointer(obj)

    fun deferenceVoidPointer(pointer: VoidPointer): Any? = globalVPM.deferencePointer(pointer)

    fun remove(pointer: VoidPointer): Boolean = globalVPM.removePointer(pointer)
    fun remove(pointer: Any?): Boolean = globalVPM.remove(pointer)
  }
}
