package no.elg.infiniteBootleg.core.util

import com.badlogic.gdx.math.Vector2
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun Vector2.dstd(v: Vector2): Double {
  val dx: Float = v.x - x
  val dy: Float = v.y - y
  return sqrt((dx * dx + dy * dy).toDouble())
}

fun Vector2.dst2(v: Vector2): Float = this.dst2(v.x, v.y)

fun Vector2.toCompactLoc(): Long = compactInt(x.roundToInt(), y.roundToInt())

fun isWithin(loc1: Long, loc2: Long, radius: Number): Boolean = isWithin(loc1.decompactLocX(), loc1.decompactLocY(), loc2.decompactLocX(), loc2.decompactLocY(), radius)

fun isWithin(
  x1: Number,
  y1: Number,
  x2: Number,
  y2: Number,
  radius: Number
): Boolean {
  val dx = x1.toDouble() - x2.toDouble()
  val dy = y1.toDouble() - y2.toDouble()
  return dx * dx + dy * dy <= radius.toDouble() * radius.toDouble()
}
