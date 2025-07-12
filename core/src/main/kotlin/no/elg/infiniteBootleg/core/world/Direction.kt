package no.elg.infiniteBootleg.core.world

import com.badlogic.gdx.utils.LongMap
import no.elg.infiniteBootleg.core.util.compactInt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.vector2i

/**
 * @author Elg
 */
enum class Direction(val dx: Int, val dy: Int) {
  CENTER(ALIGNED, ALIGNED),
  NORTH(ALIGNED, NORTHWARD),
  NORTH_EAST(EASTWARD, NORTHWARD),
  EAST(EASTWARD, ALIGNED),
  SOUTH_EAST(EASTWARD, SOUTHWARD),
  SOUTH(ALIGNED, SOUTHWARD),
  SOUTH_WEST(WESTWARD, SOUTHWARD),
  WEST(WESTWARD, ALIGNED),
  NORTH_WEST(WESTWARD, NORTHWARD);

  override fun toString(): String = "Direction[$name]{dx=$dx, dy=$dy}"

  val horizontalDirection: HorizontalDirection = HorizontalDirection.of(dx)
  val verticalDirection: VerticalDirection = VerticalDirection.of(dy)
  val compact: Long = compactInt(dx, dy)

  fun toProtoVector2i(): ProtoWorld.Vector2i =
    vector2i {
      this.x = this@Direction.dx
      this.y = this@Direction.dy
    }

  companion object {

    val CARDINAL = arrayOf(NORTH, EAST, SOUTH, WEST)
    val NON_CARDINAL = arrayOf(NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST)
    val NEIGHBORS = arrayOf(NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST, NORTH_WEST)
    private val directionMap = LongMap<Direction>()

    init {
      for (dir in entries) {
        directionMap.put(compactInt(dir.dx, dir.dy), dir)
      }
    }

    inline fun diff(from: Int, to: Int): Int =
      when {
        to > from -> -1
        to < from -> 1
        else -> ALIGNED
      }

    inline fun getVerticalDirection(fromY: Int, toY: Int): VerticalDirection = VerticalDirection.of(diff(toY, fromY))
    inline fun getHorizontalDirection(fromX: Int, toX: Int): HorizontalDirection = HorizontalDirection.of(diff(toX, fromX))

    fun direction(fromX: Int, fromY: Int, toX: Int, toY: Int): Direction {
      val diffX = diff(toX, fromX)
      val diffY = diff(toY, fromY)
      return when (diffX) {
        EASTWARD -> when (diffY) {
          NORTHWARD -> NORTH_EAST
          SOUTHWARD -> SOUTH_EAST
          else -> EAST
        }

        WESTWARD -> when (diffY) {
          NORTHWARD -> NORTH_WEST
          SOUTHWARD -> SOUTH_WEST
          else -> WEST
        }

        else -> when (diffY) {
          NORTHWARD -> NORTH
          SOUTHWARD -> SOUTH
          else -> CENTER
        }
      }
    }

    fun valueOf(dx: Int, dy: Int): Direction = directionMap[compactInt(dx, dy)] ?: throw IllegalArgumentException("No direction with dx=$dx and dy=$dy")
    fun valueOf(vec: ProtoWorld.Vector2i): Direction = valueOf(vec.x, vec.y)
  }
}
