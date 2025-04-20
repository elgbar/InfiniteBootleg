package no.elg.infiniteBootleg.core.world.generator.biome

import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.NoiseGenerator
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.createNoiseGenerator
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite.FractalType
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite.NoiseType
import kotlin.math.abs

sealed class Biome(val topBlocks: Array<Material>, val filler: Material, val biomeMaxDepth: Int, val heightNoise: NoiseGenerator, val fillerToTopBlockNoise: NoiseGenerator) {

  fun fillUpTo(
    seed: Int,
    chunk: ChunkImpl,
    localX: LocalCoord,
    localY: LocalCoord,
    height: Int
  ) {
    val blocks = chunk.blocks[localX]
    for (dy in 0 until localY) {
      val mat = materialAt(seed, height, chunk.worldX + localX, chunk.worldY + dy)
      blocks[dy] = mat.createBlock(chunk.world, chunk, localX, dy, tryRevalidateChunk = false)
    }
  }

  fun materialAt(seed: Int, height: Int, worldX: WorldCoord, worldY: WorldCoord): Material {
    val fillerHeight = fillerHeightAt(seed, worldX).toInt()
    val delta = height - worldY + fillerHeight
    return if (delta in 0 until topBlocks.size) {
      topBlocks[delta]
    } else {
      filler
    }
  }

  fun rawHeightAt(seed: Int, worldX: WorldCoord): Double = heightNoise.getNoise(seed, x = worldX)

  fun fillerHeightAt(seed: Int, worldX: WorldCoord): UInt = abs(fillerToTopBlockNoise.getNoisePositive(seed, x = worldX)).toUInt()

  companion object {
    const val INTERPOLATION_RADIUS = 25
  }

  object Plains : Biome(
    topBlocks = arrayOf(
      *Array(5) { Material.Grass },
      *Array(10) { Material.Dirt }
    ),
    filler = Material.Stone,
    biomeMaxDepth = 20,
    heightNoise = createNoiseGenerator(
      amplitude = 64.0,
      frequency = 0.009,
      noiseType = NoiseType.Value,
      fractalType = FractalType.FBm
    ),
    fillerToTopBlockNoise = createNoiseGenerator(
      amplitude = 5.0, // note: matches number of grass top blocks
      frequency = 0.02,
      noiseType = NoiseType.OpenSimplex2,
      fractalType = FractalType.FBm
    )
  )

  object Mountains : Biome(
    topBlocks = arrayOf(
      *Array(2) { Material.Grass },
      *Array(6) { Material.Dirt },
      *Array(8) { Material.Stone }
    ),
    filler = Material.Stone,
    biomeMaxDepth = 16,
    heightNoise = createNoiseGenerator(
      amplitude = 256.0,
      offset = 128.0,
      noiseType = NoiseType.OpenSimplex2,
      fractalType = FractalType.FBm,
      fractalLacunarity = 4.0,
      fractalOctaves = 4
    ),
    fillerToTopBlockNoise = createNoiseGenerator(
      amplitude = 10.0,
      frequency = 0.025
    )
  )

  object Desert : Biome(
    topBlocks = arrayOf(
      *Array(48) { Material.Sand }
    ),
    filler = Material.Sandstone,
    biomeMaxDepth = 64,
    heightNoise = createNoiseGenerator(
      amplitude = 32.0,
      offset = -32.0,
      frequency = 0.005,
      noiseType = NoiseType.Perlin,
      fractalType = FractalType.FBm,
      fractalOctaves = 4
    ),
    fillerToTopBlockNoise = createNoiseGenerator(
      amplitude = 4.0
    )
  )
}
