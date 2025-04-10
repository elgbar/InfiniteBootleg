package no.elg.infiniteBootleg.core.world

import com.badlogic.gdx.utils.LongMap
import no.elg.infiniteBootleg.core.util.compactLoc
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

  fun toProtoVector2i(): ProtoWorld.Vector2i =
    vector2i {
      this.x = this@Direction.dx
      this.y = this@Direction.dy
    }

  companion object {

    val CARDINAL = arrayOf(NORTH, EAST, SOUTH, WEST)
    val NON_CARDINAL = arrayOf(NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST)
    val NEIGHBORS = arrayOf(NORTH, EAST, SOUTH, WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST)
    private val directionMap = LongMap<Direction>()

    init {
      for (dir in entries) {
        directionMap.put(compactLoc(dir.dx, dir.dy), dir)
      }
    }

    inline fun diff(from: Int, to: Int): Int =
      if (to > from) {
        -1
      } else if (to < from) {
        1
      } else {
        ALIGNED
      }

    inline fun getVerticalDirection(fromY: Int, toY: Int): VerticalDirection = VerticalDirection.of(diff(toY, fromY))
    inline fun getHorizontalDirection(fromX: Int, toX: Int): HorizontalDirection = HorizontalDirection.of(diff(toX, fromX))

    fun direction(fromX: Int, fromY: Int, toX: Int, toY: Int): Direction {
      val diffX = diff(toX, fromX)
      val diffY = diff(toY, fromY)
      return if (diffX == EASTWARD) {
        if (diffY == NORTHWARD) {
          NORTH_EAST
        } else if (diffY == SOUTHWARD) {
          SOUTH_EAST
        } else {
          EAST
        }
      } else if (diffX == WESTWARD) {
        if (diffY == NORTHWARD) {
          NORTH_WEST
        } else if (diffY == SOUTHWARD) {
          SOUTH_WEST
        } else {
          WEST
        }
      } else {
        if (diffY == NORTHWARD) {
          NORTH
        } else if (diffY == SOUTHWARD) {
          SOUTH
        } else {
          CENTER
        }
      }
    }

    fun valueOf(dx: Int, dy: Int): Direction = directionMap[compactLoc(dx, dy)] ?: throw IllegalArgumentException("No direction with dx=$dx and dy=$dy")
    fun valueOf(vec: ProtoWorld.Vector2i): Direction = valueOf(vec.x, vec.y)
  }
}
