package no.elg.infiniteBootleg.world.render

import no.elg.infiniteBootleg.util.ChunkCompactLocGdxArray
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.world.World

interface ChunksInView {

  var horizontalStart: ChunkCoord

  var horizontalEnd: ChunkCoord

  var verticalStart: ChunkCoord

  var verticalEnd: ChunkCoord

  val horizontalLength: Int
    get() = horizontalEnd - horizontalStart
  val verticalLength: Int
    get() = verticalEnd - verticalStart
  val size: Int
    get() = horizontalLength * verticalLength

  fun isOutOfView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean {
    return chunkX < horizontalStart || chunkX >= horizontalEnd || chunkY < verticalStart || chunkY >= verticalEnd
  }

  fun isInView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean {
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

    fun ChunksInView.iterator(): LongIterator {
      val locs = ChunkCompactLocGdxArray(size)
      for (y in verticalStart until verticalEnd) {
        for (x in horizontalStart until horizontalEnd) {
          locs.add(compactLoc(x, y))
        }
      }
      return locs.toArray().iterator()
    }

    fun ChunksInView.chunkColumnsInView(): Set<ChunkCoord> = (horizontalStart..horizontalEnd).toSet()
  }
}
