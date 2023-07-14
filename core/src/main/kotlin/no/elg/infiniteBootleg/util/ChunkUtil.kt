package no.elg.infiniteBootleg.util

import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.chunks.Chunk
import kotlin.math.abs

fun Chunk.isNeighbor(other: Chunk): Boolean = isNeighbor(other.chunkX, other.chunkY)

fun Chunk.isNeighbor(chunkX: Int, chunkY: Int): Boolean {
  return if (chunkX == this.chunkX && chunkY == this.chunkY) {
    false
  } else {
    abs(chunkX - this.chunkX) <= 1 && abs(chunkY - this.chunkY) <= 1
  }
}

fun Chunk.directionTo(other: Chunk): Direction = directionTo(other.chunkX, other.chunkY)

fun Chunk.directionTo(chunkX: Int, chunkY: Int): Direction {
  return Direction.direction(this.chunkX, this.chunkY, chunkX, chunkY)
}
