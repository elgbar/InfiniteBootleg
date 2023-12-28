package no.elg.infiniteBootleg.util

import kotlin.reflect.KClass

class ClassUtils

inline fun <reified T : Any> sealedSubclassObjectInstances(): List<T> = sealedSubclassObjectInstances(T::class)
fun <T : Any> sealedSubclassObjectInstances(klazz: KClass<out T>): List<T> {
  require(klazz.isSealed) { "$klazz is not sealed" }
  return klazz.sealedSubclasses.also {
    if (it.isEmpty()) error("Sealed $klazz has no sealed subclasses")
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
}
