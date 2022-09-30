package no.elg.infiniteBootleg.util

import no.elg.infiniteBootleg.world.Chunk
import kotlin.math.abs

fun Chunk.isNeighbor(other: Chunk): Boolean = isNeighbor(other.chunkX, other.chunkY)

fun Chunk.isNeighbor(chunkX: Int, chunkY: Int): Boolean {
  return if (chunkX == this.chunkX && chunkY == this.chunkY) {
    false
  } else abs(chunkX - this.chunkX) == 1 || abs(chunkY - this.chunkY) == 1
}
