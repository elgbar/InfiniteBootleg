package no.elg.infiniteBootleg.core.util

import kotlin.reflect.KClass
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

inline fun <reified T : Any> sealedSubclassObjectInstances(): List<T> = sealedSubclassObjectInstances(T::class)

fun <T : Any> sealedSubclassObjectInstances(klazz: KClass<out T>): List<T> =
  klazz.sealedSubclasses.also {
    if (it.isEmpty()) {
      require(klazz.isSealed) { "$klazz is not sealed" }
      error("Sealed $klazz has no sealed subclasses")
    }
  }.flatMap {
    val objectInstance = it.objectInstance
    if (objectInstance == null) {
      sealedSubclassObjectInstances(it)
    } else {
      listOf(objectInstance)
    }
  }.also {
    if (it.isEmpty()) error("Sealed $klazz has no object instances")
  }

fun KProperty0<*>.isLazyInitialized(): Boolean {
  this.isAccessible = true
  val property = this.getDelegate()
  return if (property is Lazy<*>) {
    property.isInitialized()
  } else {
    error("Property $this is not lazy initialized")
  }
}
