@file:Suppress("NOTHING_TO_INLINE")

package no.elg.infiniteBootleg.core.util

const val RADIANS_TO_DEGREES: Float = 57.29578f
const val DEGREES_TO_RADIANS: Float = 0.017453292f

typealias Degrees = Float
typealias Radians = Float

inline fun Radians.toDegrees(): Degrees = this * RADIANS_TO_DEGREES
inline fun Degrees.toRadians(): Radians = this * DEGREES_TO_RADIANS

fun dst2(x1: Int, y1: Int, x2: Int, y2: Int): Int {
  val xd = x2 - x1
  val yd = y2 - y1
  return xd * xd + yd * yd
}
