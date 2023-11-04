package no.elg.infiniteBootleg.world

import com.badlogic.gdx.utils.LongMap
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.vector2i
import no.elg.infiniteBootleg.util.compactLoc
import kotlin.math.sign

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

  override fun toString(): String {
    return "Direction[$name]{dx=$dx, dy=$dy}"
  }

  val horizontalDirection = HorizontalDirection.of(dx)
  val verticalDirection = VerticalDirection.of(dy)

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

    fun direction(fromX: Int, fromY: Int, toX: Int, toY: Int): Direction {
      val diffX = sign((toX - fromX).toFloat()).toInt()
      val diffY = sign((toY - fromY).toFloat()).toInt()
      return directionMap[compactLoc(diffX, diffY)]
    }

    fun valueOf(dx: Int, dy: Int): Direction = directionMap[compactLoc(dx, dy)] ?: throw IllegalArgumentException("No direction with dx=$dx and dy=$dy")
    fun valueOf(vec: ProtoWorld.Vector2i): Direction = valueOf(vec.x, vec.y)
  }
}
