package no.elg.infiniteBootleg.core.util

import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.core.world.HorizontalDirection
import no.elg.infiniteBootleg.core.world.VerticalDirection
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import kotlin.math.abs

/**
 * Check if this chunk is a neighbor of the given chunk, this chunk is not a neighbor of itself
 */
fun Chunk.isNeighbor(other: Chunk): Boolean = isNeighbor(other.chunkX, other.chunkY)

/**
 * Check if this chunk is a neighbor of the given chunk, this chunk is not a neighbor of itself
 */
fun Chunk.isNeighbor(other: ChunkCompactLoc): Boolean = isNeighbor(other.decompactLocX(), other.decompactLocY())

/**
 * Check if this chunk is a neighbor of the given chunk, this chunk is not a neighbor of itself
 */
fun Chunk.isNeighbor(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean =
  if (chunkX == this.chunkX && chunkY == this.chunkY) {
    false
  } else {
    abs(chunkX - this.chunkX) <= 1 && abs(chunkY - this.chunkY) <= 1
  }

inline fun Chunk.directionTo(other: Chunk): Direction = directionTo(other.chunkX, other.chunkY)

inline fun Chunk.verticalDirectionTo(other: Chunk): VerticalDirection = Direction.Companion.getVerticalDirection(this.chunkY, other.chunkY)
inline fun Chunk.horizontalDirectionTo(other: Chunk): HorizontalDirection = Direction.Companion.getHorizontalDirection(this.chunkX, other.chunkX)
inline fun Chunk.directionTo(chunkX: ChunkCoord, chunkY: ChunkCoord): Direction = Direction.Companion.direction(this.chunkX, this.chunkY, chunkX, chunkY)
