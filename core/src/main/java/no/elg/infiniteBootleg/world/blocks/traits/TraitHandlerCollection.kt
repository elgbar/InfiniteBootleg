package no.elg.infiniteBootleg.world.blocks.traits

import ktx.collections.GdxIdentityMap
import kotlin.reflect.KClass

class TraitHandlerCollection : Collection<TraitHandler<out Trait>> {

  private val handlers = GdxIdentityMap<KClass<out Trait>, TraitHandler<out Trait>>()

  override val size: Int get() = handlers.size

  override fun isEmpty(): Boolean = handlers.isEmpty

  override fun iterator(): Iterator<TraitHandler<out Trait>> = handlers.values().iterator()

  override fun containsAll(elements: Collection<TraitHandler<out Trait>>): Boolean {
    for (element in elements) {
      if (element !in this) {
        return false
      }
    }
    return true
  }

  override fun contains(element: TraitHandler<out Trait>): Boolean = handlers.values().contains(element)

  operator fun <T : Trait> get(trait: KClass<out T>): TraitHandler<T>? {
    @Suppress("UNCHECKED_CAST")
    return handlers.get(trait) as TraitHandler<T>?
  }

  operator fun <T : Trait> get(trait: Class<out T>): TraitHandler<T>? {
    return get(trait.kotlin)
  }

  inline fun <reified T : Trait> get(): TraitHandler<T>? {
    return get(T::class)
  }

  operator fun <T : Trait> set(traitClass: KClass<T>, traitHandler: TraitHandler<T>) {
    handlers.put(traitClass, traitHandler)
  }

  operator fun <T : Trait> set(traitClass: Class<T>, traitHandler: TraitHandler<T>) {
    set(traitClass.kotlin, traitHandler)
  }

  inline fun <reified T : Trait> set(traitHandler: TraitHandler<T>) {
    set(T::class, traitHandler)
  }
}
