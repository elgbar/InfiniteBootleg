package no.elg.infiniteBootleg.core.world.generator.biome

import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.NoiseGenerator
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.createNoiseGenerator
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.core.world.generator.chunk.PerlinChunkGenerator
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite.FractalType
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite.NoiseType
import kotlin.math.abs
import kotlin.math.floor

sealed class Biome(val topmostBlock: Material, val topBlocks: Array<Material>, val filler: Material, val heightNoise: NoiseGenerator, val fillerToTopBlockNoise: NoiseGenerator) {

//  fun noise(
// // seed: Int = 1337,
//    frequency: Double = 0.01,
//    noiseType: NoiseType = NoiseType.OpenSimplex2,
//    rotationType3D: RotationType3D = RotationType3D.None,
// // transformType3D: TransformType3D = TransformType3D.DefaultOpenSimplex2,
//    fractalType: FractalType = FractalType.None,
//    octaves: Int = 3,
//    lacunarity: Double = 2.0,
//    gain: Double = 0.5,
//    weightedStrength: Double = 0.0,
//    pingPongStrength: Double = 2.0,
//    fractalBounding: Double = 1 / 1.75,
//    cellularDistanceFunction: CellularDistanceFunction = CellularDistanceFunction.EuclideanSq,
//    cellularReturnType: CellularReturnType = CellularReturnType.Distance,
//    cellularJitterModifier: Double = 1.0,
//    domainWarpType: DomainWarpType = DomainWarpType.OpenSimplex2,
// // warpTransformType3D: TransformType3D = TransformType3D.DefaultOpenSimplex2,
//    domainWarpAmp: Double = 1.0,
//  ): FastNoiseLite {
//    FastNoiseLite(0).apply {
//      setNoiseType(FastNoiseLite.NoiseType.Perlin)
//      setFrequency(frequency)
//      setFractalType(FastNoiseLite.FractalType.FBm)
//      setFractalOctaves(2)
//      setFractalLacunarity(2.0)
//      setFractalGain(0.5)
//    }
//  }
//
//  val heightNoise: FastNoiseLite = FastNoiseLite(0).apply {
//    setNoiseType(FastNoiseLite.NoiseType.Perlin)
//    setFrequency(frequency)
//    setFractalType(FastNoiseLite.FractalType.FBm)
//    setFractalOctaves(2)
//    setFractalLacunarity(2.0)
//    setFractalGain(0.5)
//
//  }
//
//
//
//  val fillerToTopBlockNoise: FastNoiseLite = FastNoiseLite(0).apply {
//    setNoiseType(FastNoiseLite.NoiseType.Perlin)
//    setFrequency(frequency * 2)
//    createNoiseGenerator(
//      noiseType = NoiseType.Perlin,
//    )
//  }

  fun heightAt(pcg: PerlinChunkGenerator, worldX: WorldCoord): Int {
    var y = 0
    val seed = pcg.seed.toInt()
    for (dx in -INTERPOLATION_RADIUS..INTERPOLATION_RADIUS) {
      y = if (dx != 0) {
        val biome = pcg.getBiome(worldX + dx)
        (y + biome.rawHeightAt(seed, worldX + dx)).toInt()
      } else {
        (y + rawHeightAt(seed, worldX)).toInt()
      }
    }
    val finalY = y / (INTERPOLATION_RADIUS * 2 + 1)
    return finalY
  }

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

  fun rawHeightAt(seed: Int, worldX: WorldCoord): Double = heightNoise.getNoise(seed, x = worldX)

  fun fillerHeightAt(seed: Int, worldX: WorldCoord): Double = fillerToTopBlockNoise.getNoise(seed, x = worldX)

  companion object {
    const val INTERPOLATION_RADIUS = 25
  }

  object Plains : Biome(
    topmostBlock = Material.Grass,
    topBlocks = arrayOf<Material>(
      *Array(4) { Material.Grass },
      *Array(10) { Material.Dirt }
    ),
    filler = Material.Stone,
    heightNoise = createNoiseGenerator(
      amplitude = 64.0,
      frequency = 0.009,
      noiseType = NoiseType.Perlin,
      fractalType = FractalType.FBm,
      fractalOctaves = 2
    ),
    fillerToTopBlockNoise = createNoiseGenerator(
      amplitude = 10.0,
      frequency = 0.018,
      noiseType = NoiseType.Perlin
    )
  )

  object Mountains : Biome(
    topmostBlock = Material.Grass,
    topBlocks = arrayOf<Material>(
      *Array(2) { Material.Grass },
      *Array(6) { Material.Dirt }
    ),
    filler = Material.Stone,
    heightNoise = createNoiseGenerator(
      amplitude = 356.0,
      offset = 25.0,
      noiseType = NoiseType.Perlin,
      fractalType = FractalType.FBm,
      fractalOctaves = 2
    ),
    fillerToTopBlockNoise = createNoiseGenerator(
      amplitude = 10.0,
      frequency = 0.02,
      noiseType = NoiseType.Perlin
    )
  )

  object Desert : Biome(
    topmostBlock = Material.Sand,
    topBlocks = arrayOf<Material>(
      *Array(12) { Material.Sand }
    ),
    filler = Material.Sandstone,

    heightNoise = createNoiseGenerator(
      amplitude = 32.0,
      frequency = 0.005,
      noiseType = NoiseType.Perlin,
      fractalType = FractalType.FBm,
      fractalOctaves = 2
    ),
    fillerToTopBlockNoise = createNoiseGenerator(
      amplitude = 10.0,
      noiseType = NoiseType.Perlin
    )
  )
}
