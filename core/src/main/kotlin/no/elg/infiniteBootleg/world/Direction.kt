package no.elg.infiniteBootleg.world

import com.badlogic.gdx.utils.LongMap
import no.elg.infiniteBootleg.util.compactLoc
import kotlin.math.sign

/**
 * @author Elg
 */
enum class Direction(val dx: Int, val dy: Int) {
  CENTER(0, 0),
  NORTH(0, 1),
  NORTH_EAST(1, 1),
  EAST(1, 0),
  SOUTH_EAST(1, -1),
  SOUTH(0, -1),
  SOUTH_WEST(-1, -1),
  WEST(-1, 0),
  NORTH_WEST(-1, 1);

  override fun toString(): String {
    return "Direction[$name]{dx=$dx, dy=$dy}"
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
  }
}
