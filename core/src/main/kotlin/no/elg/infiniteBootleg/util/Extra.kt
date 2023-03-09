package no.elg.infiniteBootleg.util

import com.fasterxml.uuid.Generators
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.Material
import java.util.UUID
import kotlin.contracts.contract

fun fromUUIDOrNull(string: String?): UUID? {
  return try {
    UUID.fromString(string)
  } catch (e: Exception) {
    null
  }
}

private val namespace = UUID.fromString("1aeeb167-a72e-45d3-8b75-8a144e56ca54")
private val uuidv5Generator = Generators.nameBasedGenerator(namespace)

fun generateUUIDFromString(string: String): UUID {
  return uuidv5Generator.generate(string)
}

fun generateUUIDFromLong(long: Long): UUID {
  return uuidv5Generator.generate(long.toString())
}

fun StringBuilder.fastIntFormat(int: Int, d: Byte): StringBuilder {
  val len = int.stringSize()
  this.ensureCapacity(d.toInt())
  if (len < d) {
    for (i in 1..(d - len)) {
      this.append(' ')
    }
  }
  append(int)
  return this
}

/**
 * Returns the string representation size for a given int value.
 *
 * @param x int value
 * @return string size
 *
 * @implNote There are other ways to compute this: e.g. binary search,
 * but values are biased heavily towards zero, and therefore linear search
 * wins. The iteration results are also routinely inlined in the generated
 * code after loop unrolling.
 */
fun Int.stringSize(): Int {
  var x = this
  var d = 1
  if (x >= 0) {
    d = 0
    x = -x
  }
  var p = -10
  for (i in 1..9) {
    if (x > p) return i + d
    p *= 10
  }
  return 10 + d
}

fun Block.isAir(): Boolean = this.material == Material.AIR

@JvmName("isAirOrNull")
fun Block?.isAir(): Boolean = this == null || this.material == Material.AIR

@JvmName("isNotAirOrNull")
fun Block?.isNotAir(): Boolean {
  contract { returns(true) implies (this@isNotAir != null) }
  return this != null && this.material != Material.AIR
}

fun Block.isNotAir(): Boolean {
  return this.material != Material.AIR
}
