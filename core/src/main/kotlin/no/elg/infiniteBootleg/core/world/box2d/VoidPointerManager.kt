package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.gdx.jnigen.runtime.pointer.VoidPointer
import io.github.oshai.kotlinlogging.KotlinLogging
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.world.BOX2D_LOCK

private val logger = KotlinLogging.logger {}

/**
 * Not thread-safe
 */
class VoidPointerManager {

  private var id: Long = START_ID
  private val addrToObj = Long2ObjectOpenHashMap<Any>()
  private val objToAddr = Object2LongOpenHashMap<Any>().also { it.defaultReturnValue(NOT_IN_MANAGER) }

  private fun nextId(): Long = id++

  /**
   * Create a new pointer to the given object.
   * If the object is null, a null pointer will be returned
   */
  fun createPointer(obj: Any?): VoidPointer {
    ThreadType.requireCorrectThreadType(ThreadType.PHYSICS)
    if (obj == null) {
      return VoidPointer.NULL
    }
    val existingPointer = objToAddr.getLong(obj)
    return if (existingPointer != NOT_IN_MANAGER) {
      
      VoidPointer(existingPointer, false)
    } else {
      val id = nextId()
      objToAddr.put(obj, id)
      addrToObj.put(id, obj)
      VoidPointer(id, false)
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
      is VoidPointer -> this@VoidPointerManager.removePointer(obj)
      else -> removeObject(obj)
    }

  fun deferencePointer(pointer: VoidPointer): Any? {
    if (pointer.isInvalid) {
      return null
    }
    val id = pointer.getPointer()
    return addrToObj.get(id)
  }

  fun clean() {
    synchronized(BOX2D_LOCK) {
      if (addrToObj.isNotEmpty() || objToAddr.isNotEmpty()) {
        logger.warn { "Memory leak: still referencing objects" }
        logger.info { "Memory leak (${addrToObj.size}): Objects: $addrToObj" }
        logger.info { "Memory leak (${objToAddr.size}): Addresses: $objToAddr" }
      }
      addrToObj.clear()
      objToAddr.clear()
      id = START_ID
    }
  }

  companion object {
    private const val NOT_IN_MANAGER = Long.MIN_VALUE

    /**
     * Start at 1 to avoid 0 being a valid pointer, which is used for null pointers
     */
    private const val START_ID = 1L

    // TODO make a VoidPointerManager per world
    val globalVPM: VoidPointerManager = VoidPointerManager()

    fun createVoidPointer(obj: Any?): VoidPointer = globalVPM.createPointer(obj)

    fun deferenceVoidPointer(pointer: VoidPointer): Any? = globalVPM.deferencePointer(pointer)

    fun removePointer(pointer: VoidPointer): Boolean = globalVPM.removePointer(pointer)
    fun remove(pointer: Any?): Boolean = globalVPM.remove(pointer)
  }
}
