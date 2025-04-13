package no.elg.infiniteBootleg.core.world.generator.biome

import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.getNoise
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.core.world.generator.chunk.PerlinChunkGenerator
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite
import kotlin.math.abs
import kotlin.math.floor

sealed class Biome(val y: Double, val amplitude: Double, val frequency: Double, val offset: Int, val filler: Material, val topmostBlock: Material, val topBlocks: Array<Material>) {

  val heightNoise: FastNoiseLite = FastNoiseLite(0).apply {
    setNoiseType(FastNoiseLite.NoiseType.Perlin)
    setFrequency(frequency)
    setFractalType(FastNoiseLite.FractalType.FBm)
    setFractalOctaves(6)
    setFractalLacunarity(2.0)
    setFractalGain(0.5)
  }

  val fillerToTopBlockNoise: FastNoiseLite = FastNoiseLite(0).apply {
    setNoiseType(FastNoiseLite.NoiseType.Perlin)
    setFrequency(frequency)
    setFractalType(FastNoiseLite.FractalType.FBm)
    setFractalOctaves(6)
    setFractalLacunarity(2.0)
    setFractalGain(0.05)
  }

  fun heightAt(pcg: PerlinChunkGenerator, worldX: WorldCoord): Int {
    var y = 0
    for (dx in -INTERPOLATION_RADIUS..INTERPOLATION_RADIUS) {
      y = if (dx != 0) {
        val biome = pcg.getBiome(worldX + dx)
        (y + biome.rawHeightAt(pcg.seed, worldX + dx)).toInt()
      } else {
        (y + rawHeightAt(pcg.seed, worldX)).toInt()
      }
    }
    val finalY = y / (INTERPOLATION_RADIUS * 2 + 1)
    return finalY
  }

  fun fillUpTo(
    seed: Long,
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

  fun materialAt(seed: Long, height: Int, worldX: WorldCoord, worldY: WorldCoord): Material {
    var delta = height - worldY
    if (delta == 0) {
      return topmostBlock
    }
    delta += abs(floor(fillerHeightAt(seed, worldX)).toInt())
    return if (delta >= topBlocks.size) {
      filler
    } else {
      topBlocks[delta]
    }
  }

  fun rawHeightAt(seed: Long, worldX: WorldCoord): Double {
    heightNoise.setSeed(seed.toInt())
    return heightNoise.getNoise(x = worldX, y = y, amplitude = amplitude) + offset
  }

  fun fillerHeightAt(seed: Long, worldX: WorldCoord): Double {
    fillerToTopBlockNoise.setSeed(seed.toInt())
    return fillerToTopBlockNoise.getNoise(x = worldX, y = y, amplitude = 10.0)
  }

  companion object {
    const val INTERPOLATION_RADIUS = 25
  }

  object Plains : Biome(
    y = 0.1,
    amplitude = 64.0,
    frequency = 0.009,
    offset = 0,
    filler = Material.Stone,
    topmostBlock = Material.Grass,
    topBlocks = arrayOf<Material>(
      *Array(4) { Material.Grass },
      *Array(10) { Material.Dirt }
    )
  )

  object Mountains : Biome(
    y = 100.0,
    amplitude = 356.0,
    frequency = 0.01,
    offset = 25,
    filler = Material.Stone,
    topmostBlock = Material.Grass,
    topBlocks = arrayOf<Material>(
      *Array(2) { Material.Grass },
      *Array(6) { Material.Dirt }
    )
  )

  object Desert : Biome(
    y = 0.1,
    amplitude = 32.0,
    frequency = 0.005,
    offset = 0,
    filler = Material.Sandstone,
    topmostBlock = Material.Sand,
    topBlocks = arrayOf<Material>(
      *Array(12) { Material.Sand }
    )
  )
}
