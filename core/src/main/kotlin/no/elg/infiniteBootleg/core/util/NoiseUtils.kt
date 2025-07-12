package no.elg.infiniteBootleg.core.util

import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite.CellularDistanceFunction
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite.CellularReturnType
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite.DomainWarpType
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite.FractalType
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite.NoiseType
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite.RotationType3D

/**
 * @return Noise output bounded between 0..[amplitude]
 */
fun FastNoiseLite.getNoisePositive(x: Number, y: Number, amplitude: Double = 1.0): Double {
  @Suppress("DEPRECATION")
  val randomNumber = (1.0 + GetNoise(x.toDouble(), y.toDouble())) / 2.0
  return randomNumber * amplitude
}

/**
 * @return Noise output bounded between -[amplitude]..[amplitude]
 */
@Suppress("DEPRECATION")
fun FastNoiseLite.getNoise(x: Number, y: Number, amplitude: Double = 1.0): Double = GetNoise(x.toDouble(), y.toDouble()) * amplitude

/**
 * @return Noise output bounded between -[amplitude]..[amplitude]
 */
@Suppress("DEPRECATION")
fun FastNoiseLite.getNoise(x: Number, y: Number, z: Number, amplitude: Double = 1.0): Double = GetNoise(x.toDouble(), y.toDouble(), z.toDouble()) * amplitude

/**
 * @return Noise output bounded between 0..[amplitude]
 */
fun FastNoiseLite.getNoisePositive(x: Number, y: Number, z: Number, amplitude: Double = 1.0): Double {
  @Suppress("DEPRECATION")
  val randomNumber = (1.0 + GetNoise(x.toDouble(), y.toDouble(), z.toDouble())) / 2.0
  return randomNumber * amplitude
}

data class NoiseGenerator(val generator: FastNoiseLite, val amplitude: Double, val offset: Double) {
  fun getNoise(seed: Int, x: Number, y: Number = 0): Double {
    generator.setSeed(seed)
    return generator.getNoise(x, y, amplitude) + offset
  }

  fun getNoise(seed: Int, x: Number, y: Number, z: Number): Double {
    generator.setSeed(seed)
    return generator.getNoise(x, y, z, amplitude) + offset
  }

  fun getNoisePositive(seed: Int, x: Number, y: Number = 0): Double {
    generator.setSeed(seed)
    return generator.getNoisePositive(x, y, amplitude) + offset
  }

  fun getNoisePositive(seed: Int, x: Number, y: Number, z: Number): Double {
    generator.setSeed(seed)
    return generator.getNoisePositive(x, y, z, amplitude) + offset
  }
}

fun createNoiseGenerator(
  amplitude: Double = 1.0,
  offset: Double = 0.0,
  noiseType: NoiseType = NoiseType.OpenSimplex2,
  frequency: Double = 0.01,
  rotationType3D: RotationType3D = RotationType3D.None,
  fractalType: FractalType = FractalType.None,
  fractalOctaves: Int = 3,
  fractalLacunarity: Double = 2.0,
  fractalGain: Double = 0.5,
  fractalWeightedStrength: Double = 0.0,
  fractalPingPongStrength: Double = 2.0,
  cellularDistanceFunction: CellularDistanceFunction = CellularDistanceFunction.EuclideanSq,
  cellularReturnType: CellularReturnType = CellularReturnType.Distance,
  cellularJitter: Double = 1.0,
  domainWarpType: DomainWarpType = DomainWarpType.OpenSimplex2,
  domainWarpAmp: Double = 1.0
): NoiseGenerator {
  val generator = FastNoiseLite().apply {
    setNoiseType(noiseType)
    setFrequency(frequency)
    setRotationType3D(rotationType3D)

    // fractal
    setFractalType(fractalType)
    setFractalOctaves(fractalOctaves)
    setFractalLacunarity(fractalLacunarity)
    setFractalGain(fractalGain)
    setFractalWeightedStrength(fractalWeightedStrength)
    setFractalPingPongStrength(fractalPingPongStrength)

    // cellular
    setCellularDistanceFunction(cellularDistanceFunction)
    setCellularReturnType(cellularReturnType)
    setCellularJitter(cellularJitter)

    // Domain wrap
    setDomainWarpType(domainWarpType)
    setDomainWarpAmp(domainWarpAmp)
  }

  return NoiseGenerator(generator, amplitude, offset)
}
