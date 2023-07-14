package no.elg.infiniteBootleg.world.render

import com.badlogic.gdx.utils.OrderedSet
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.world.World

interface ChunksInView {

  var horizontalStart: Int

  var horizontalEnd: Int

  var verticalStart: Int

  var verticalEnd: Int

  val horizontalLength: Int
    get() = horizontalEnd - horizontalStart
  val verticalLength: Int
    get() = verticalEnd - verticalStart
  val size: Int
    get() = horizontalLength * verticalLength

  fun isOutOfView(chunkX: Int, chunkY: Int): Boolean {
    return chunkX < horizontalStart || chunkX >= horizontalEnd || chunkY < verticalStart || chunkY >= verticalEnd
  }

  fun isInView(chunkX: Int, chunkY: Int): Boolean {
    return (chunkX in horizontalStart until horizontalEnd) && (chunkY in verticalStart until verticalEnd)
  }

  companion object {
    /**
     * @param world The chunk in the world to iterator over
     * @return An iterator for the given world
     */
    inline fun ChunksInView.forEach(world: World, crossinline apply: (chunk: Chunk) -> Unit) {
      for (y in verticalStart until verticalEnd) {
        for (x in horizontalStart until horizontalEnd) {
          val chunk = world.getChunk(x, y, true) ?: continue
          apply(chunk)
        }
      }
    }

    fun ChunksInView.toSet(): OrderedSet<Location> {
      val locs = OrderedSet<Location>(size)
      locs.orderedItems().ordered = false
      for (y in verticalStart until verticalEnd) {
        for (x in horizontalStart until horizontalEnd) {
          locs.add(Location(x, y))
        }
      }
      return locs
    }
  }
}
