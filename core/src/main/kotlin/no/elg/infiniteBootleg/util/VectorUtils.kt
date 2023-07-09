package no.elg.infiniteBootleg.util

import com.badlogic.gdx.math.Vector2
import kotlin.math.sqrt

fun Vector2.dstd(v: Vector2): Double {
  val dx: Float = v.x - x
  val dy: Float = v.y - y
  return sqrt((dx * dx + dy * dy).toDouble())
}
