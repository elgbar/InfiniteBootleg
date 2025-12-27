package no.elg.infiniteBootleg.core.world.render

import no.elg.infiniteBootleg.core.util.ChunkCompactLoc
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.compactInt
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.world.World

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

  fun isOutOfView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean = !isInView(chunkX, chunkY)

  fun isInView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean = (chunkX in horizontalStart..<horizontalEnd) && (chunkY in verticalStart..<verticalEnd)

  companion object {
    /**
     * @param world The chunk in the world to iterator over
     * @return An iterator for the given world
     */
    fun ChunksInView.forEach(world: World, apply: (chunk: Chunk) -> Unit) {
      for (y in verticalStart until verticalEnd) {
        for (x in horizontalStart until horizontalEnd) {
          val chunk = world.getChunk(x, y, true) ?: continue
          apply(chunk)
        }
      }
    }

    fun ChunksInView.sequence(): Sequence<ChunkCompactLoc> =
      sequence {
        for (y in verticalStart until verticalEnd) {
          for (x in horizontalStart until horizontalEnd) {
            yield(compactInt(x, y))
          }
        }
      }

    fun ChunksInView.chunkColumnsInView(): Set<ChunkCoord> = (horizontalStart..horizontalEnd).toSet()
  }
}
