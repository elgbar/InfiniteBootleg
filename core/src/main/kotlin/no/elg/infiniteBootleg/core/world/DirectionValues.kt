package no.elg.infiniteBootleg.core.world

/**
 * The X coordinate is to the west
 */
const val WESTWARD = -1

/**
 * The X coordinate is to the east
 */
const val EASTWARD = 1

/**
 * The Y coordinate is to the north
 */
const val NORTHWARD = 1

/**
 * The Y coordinate is to the south
 */
const val SOUTHWARD = -1

/**
 * The X or Y coordinate is aligned
 */
const val ALIGNED = 0

enum class HorizontalDirection(val value: Int) {
  WESTWARD(no.elg.infiniteBootleg.core.world.WESTWARD),
  HORIZONTALLY_ALIGNED(ALIGNED),
  EASTWARD(no.elg.infiniteBootleg.core.world.EASTWARD);

  companion object {
    fun of(dx: Int): HorizontalDirection =
      when {
        dx < 0 -> WESTWARD
        dx > 0 -> EASTWARD
        else -> HORIZONTALLY_ALIGNED
      }
  }
}

enum class VerticalDirection(val value: Int) {
  NORTHWARD(no.elg.infiniteBootleg.core.world.NORTHWARD),
  VERTICALLY_ALIGNED(ALIGNED),
  SOUTHWARD(no.elg.infiniteBootleg.core.world.SOUTHWARD);

  companion object {
    fun of(dx: Int): VerticalDirection =
      when {
        dx > 0 -> NORTHWARD
        dx < 0 -> SOUTHWARD
        else -> VERTICALLY_ALIGNED
      }
  }
}
