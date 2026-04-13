package no.elg.infiniteBootleg.core.util

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentType
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.utils.Bits
import com.badlogic.gdx.utils.ObjectMap
import no.elg.infiniteBootleg.core.util.FamilyComponents.Companion.toFamilyComponents
import java.util.Collections.disjoint
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.staticProperties
import kotlin.reflect.jvm.isAccessible

@Suppress("UNCHECKED_CAST")
val classToComponentType: ObjectMap<Class<out Component>, ComponentType> =
  ComponentType::class.staticProperties.first { it.name == "assignedComponentTypes" }.also { it.isAccessible = true }.call() as ObjectMap<Class<out Component>, ComponentType>

fun findComponentClassFromIndex(index: Int): Class<out Component>? = classToComponentType.firstOrNull { entry -> entry.value.index == index }?.key

@Suppress("UNCHECKED_CAST")
object FamilyExtras {

  private val declaredMembers = Family::class.memberProperties.apply { forEach { it.isAccessible = true } }
  val allBitsProperty: KProperty1<Family, Bits> = declaredMembers.first { it.name == "all" } as KProperty1<Family, Bits>
  val oneBitsProperty: KProperty1<Family, Bits> = declaredMembers.first { it.name == "one" } as KProperty1<Family, Bits>
  val excludeBitsProperty: KProperty1<Family, Bits> = declaredMembers.first { it.name == "exclude" } as KProperty1<Family, Bits>
}

data class FamilyComponents(val all: Set<Class<out Component>>, val one: Set<Class<out Component>>, val exclude: Set<Class<out Component>>) {

  fun buildString(sb: StringBuilder): StringBuilder {
    fun appendComps(type: String, set: Set<Class<out Component>>) {
      if (set.isNotEmpty()) {
        sb.append("{").append(type).append(":")
        set.sortedBy { it.displayName }.joinTo(sb) { it.displayName }
        sb.append("}")
      }
    }
    appendComps("all", all)
    appendComps("one", one)
    appendComps("exclude", exclude)
    return sb
  }

  override fun toString(): String = StringBuilder().also(::buildString).toString()

  fun matchesString(entity: Entity, sb: StringBuilder = StringBuilder()): Boolean {
    var matches = true
    val entityComponents: Set<Class<out Component>> = entity.components.mapTo(mutableSetOf()) { it.javaClass }

    if (!entityComponents.containsAll(all)) {
      sb.append("The 'all' part of the family does not match, entity is missing the components ")
      all.filter { it !in entityComponents }.sortedBy { it.displayName }.joinTo(sb, prefix = "[", postfix = "]") { it.displayName }
      matches = false
    }

    if (one.isNotEmpty() && !disjoint(one, entityComponents)) {
      if (matches) {
        matches = false
      } else {
        sb.append("\n")
      }
      sb.append("The 'one' part of the family does not match, entity does not have any of the components ")
      one.sortedBy { it.displayName }.joinTo(sb, prefix = "[", postfix = "]") { it.displayName }
    }

    if (exclude.isNotEmpty() && disjoint(exclude, entityComponents)) {
      if (matches) {
        matches = false
      } else {
        sb.append("\n")
      }
      sb.append("The 'exclude' part of the family does not match, entity has excluded the components ")
      exclude.filter { it in entityComponents }.sortedBy { it.displayName }.joinTo(sb, prefix = "[", postfix = "]") { it.displayName }
    }

    return matches
  }

  companion object {
    private fun convertFamilyBitsToComponentsSet(bits: Bits): Set<Class<out Component>> {
      val numBits = bits.length()
      val set = mutableSetOf<Class<out Component>>()
      for (i in 0..<numBits) {
        if (bits.get(i)) {
          set.add(findComponentClassFromIndex(i) ?: continue)
        }
      }
      return set
    }

    fun Family.toFamilyComponents(): FamilyComponents {
      val all = convertFamilyBitsToComponentsSet(FamilyExtras.allBitsProperty(this))
      val one = convertFamilyBitsToComponentsSet(FamilyExtras.oneBitsProperty(this))
      val exclude = convertFamilyBitsToComponentsSet(FamilyExtras.excludeBitsProperty(this))
      return FamilyComponents(all, one, exclude)
    }
  }
}

fun Family.toComponentsString(): String {
  val sb = StringBuilder("Entity family components [").append(toFamilyComponents().toString()).append("]")
  return sb.toString()
}
