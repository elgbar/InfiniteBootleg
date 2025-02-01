package no.elg.infiniteBootleg.util

import io.mockk.every
import io.mockk.mockk
import no.elg.infiniteBootleg.core.util.directionTo
import no.elg.infiniteBootleg.core.util.isNeighbor
import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.world.chunks.Chunk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ChunkUtilKtTest {

  private fun mockChunk(dir: Direction, mod: Int = 1): Chunk {
    val originChunk = mockk<Chunk>()
    every { originChunk.chunkX } returns dir.dx * mod
    every { originChunk.chunkY } returns dir.dy * mod
    return originChunk
  }

  private val originChunk by lazy {
    val originChunk = mockk<Chunk>()
    every { originChunk.chunkX } returns 0
    every { originChunk.chunkY } returns 0
    originChunk
  }

  @ParameterizedTest
  @EnumSource(value = Direction::class, names = ["CENTER"], mode = EnumSource.Mode.EXCLUDE)
  fun reflexive_isNeighbor(dir: Direction) {
    val chunkDir = mockChunk(dir)
    assertTrue(chunkDir.isNeighbor(originChunk))
    assertTrue(originChunk.isNeighbor(chunkDir))
  }

  @ParameterizedTest
  @EnumSource(value = Direction::class, names = ["CENTER"], mode = EnumSource.Mode.EXCLUDE)
  fun `chunks more than one away are not neighbors`(dir: Direction) {
    val chunkDir = mockChunk(dir, 2)
    assertFalse(chunkDir.isNeighbor(originChunk))
    assertFalse(originChunk.isNeighbor(chunkDir))
  }

  @Test
  fun `chunk is not neighbor with itself`() {
    assertFalse(originChunk.isNeighbor(originChunk))
  }

  @ParameterizedTest
  @EnumSource(value = Direction::class)
  fun directionTo(dir: Direction) {
    val chunkDir = mockChunk(dir)
    assertEquals(dir, originChunk.directionTo(chunkDir))
  }

  @ParameterizedTest
  @EnumSource(value = Direction::class, names = ["CENTER"], mode = EnumSource.Mode.EXCLUDE)
  fun directionTo_far(dir: Direction) {
    val chunkDir = mockChunk(dir, 2)
    assertEquals(dir, originChunk.directionTo(chunkDir))
  }
}
