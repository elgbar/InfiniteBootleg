package no.elg.infiniteBootleg.world

import com.badlogic.gdx.math.Vector2
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2i
import no.elg.infiniteBootleg.util.compactLoc

/**
 * Immutable 2D vector in integer space
 *
 * @author Elg
 */
class Location(val x: Int, val y: Int) {
  fun scl(x: Int, y: Int): Location {
    return Location(this.x * x, this.y * y)
  }

  fun dist(loc: Location): Double {
    return Math.sqrt(distCubed(loc).toDouble())
  }

  fun distCubed(loc: Location): Long {
    return distCubed(x, y, loc.x, loc.y)
  }

  fun toVector2(): Vector2 {
    return Vector2(x.toFloat(), y.toFloat())
  }

  fun relative(dir: Direction): Location {
    return Location(x + dir.dx, y + dir.dy)
  }

  fun toVector2i(): Vector2i {
    return Vector2i.newBuilder().setX(x).setY(y).build()
  }

  /**
   * @return This location compacted into a single long
   * @see compactLoc
   */
  fun toCompactLocation(): Long {
    return compactLoc(x, y)
  }

  override fun hashCode(): Int {
    return 31 * x + y
  }

  override fun equals(o: Any?): Boolean {
    if (this === o) {
      return true
    }
    return if (o is Location) {
      y == o.y && x == o.x
    } else {
      false
    }
  }

  override fun toString(): String {
    return "($x, $y)"
  }

  companion object {
    fun relative(x: Int, y: Int, dir: Direction): Location {
      return Location(x + dir.dx, y + dir.dy)
    }

    fun relativeCompact(x: Int, y: Int, dir: Direction): Long {
      return compactLoc(x + dir.dx, y + dir.dy)
    }

    fun distCubed(x1: Int, y1: Int, x2: Int, y2: Int): Long {
      return (x2 - x1).toLong() * (x2 - x1) + (y2 - y1).toLong() * (y2 - y1)
    }

    fun distCubed(x1: Double, y1: Double, x2: Double, y2: Double): Double {
      return (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)
    }

    fun fromVector2i(vector2i: Vector2i): Location {
      return Location(vector2i.x, vector2i.y)
    }
  }
}
