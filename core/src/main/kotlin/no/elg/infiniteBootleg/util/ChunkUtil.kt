package no.elg.infiniteBootleg.util

import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.HorizontalDirection
import no.elg.infiniteBootleg.world.VerticalDirection
import no.elg.infiniteBootleg.world.chunks.Chunk
import kotlin.math.abs

/**
 * Check if this chunk is a neighbor of the given chunk, this chunk is not a neighbor of itself
 */
fun Chunk.isNeighbor(other: Chunk): Boolean = isNeighbor(other.chunkX, other.chunkY)

/**
 * Check if this chunk is a neighbor of the given chunk, this chunk is not a neighbor of itself
 */
fun Chunk.isNeighbor(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean {
  return if (chunkX == this.chunkX && chunkY == this.chunkY) {
    false
  } else {
    abs(chunkX - this.chunkX) <= 1 && abs(chunkY - this.chunkY) <= 1
  }
}

inline fun Chunk.directionTo(other: Chunk): Direction = directionTo(other.chunkX, other.chunkY)

inline fun Chunk.verticalDirectionTo(other: Chunk): VerticalDirection = Direction.getVerticalDirection(this.chunkY, other.chunkY)
inline fun Chunk.horizontalDirectionTo(other: Chunk): HorizontalDirection = Direction.getHorizontalDirection(this.chunkY, other.chunkY)
inline fun Chunk.directionTo(chunkX: ChunkCoord, chunkY: ChunkCoord): Direction = Direction.direction(this.chunkX, this.chunkY, chunkX, chunkY)
