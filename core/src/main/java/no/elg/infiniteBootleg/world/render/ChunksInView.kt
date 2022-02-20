package no.elg.infiniteBootleg.world.render

import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.World

/** @author Elg
 */
class ChunksInView {
  @JvmField
  var horizontalStart = 0

  @JvmField
  var horizontalEnd = 0

  @JvmField
  var verticalStart = 0

  @JvmField
  var verticalEnd = 0

  val horizontalLength: Int
    get() = horizontalEnd - horizontalStart
  val verticalLength: Int
    get() = verticalEnd - verticalStart
  val chunksInView: Int
    get() = horizontalLength * verticalLength

  fun isOutOfView(chunkX: Int, chunkY: Int): Boolean {
    return chunkX < horizontalStart || chunkX >= horizontalEnd || chunkY < verticalStart || chunkY >= verticalEnd
  }

  /**
   * @param world The chunk in the world to iterator over
   * @return An iterator for the given world
   */
  inline fun forEach(world: World, crossinline apply: (chunk: Chunk) -> Unit) {
    for (y in verticalStart until verticalEnd) {
      for (x in horizontalStart until horizontalEnd) {
        val chunk = world.getChunk(x, y) ?: continue
        apply(chunk)
      }
    }
  }
}
