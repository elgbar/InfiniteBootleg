package no.elg.infiniteBootleg.core.util

import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite

/**
 * @return Noise output bounded between 0..[amplitude]
 */
fun FastNoiseLite.getNoisePositive(x: Number, y: Number, amplitude: Double = 1.0): Double {
  val randomNumber = (1.0 + GetNoise(x.toDouble(), y.toDouble())) / 2.0
  return randomNumber * amplitude
}

/**
 * @return Noise output bounded between -[amplitude]..[amplitude]
 */
fun FastNoiseLite.getNoise(x: Number, y: Number, amplitude: Double = 1.0): Double = GetNoise(x.toDouble(), y.toDouble()) * amplitude

/**
 * @return Noise output bounded between -[amplitude]..[amplitude]
 */
fun FastNoiseLite.getNoise(x: Number, y: Number, z: Number, amplitude: Double = 1.0): Double = GetNoise(x.toDouble(), y.toDouble(), z.toDouble()) * amplitude

/**
 * @return Noise output bounded between 0..[amplitude]
 */
fun FastNoiseLite.getNoisePositive(x: Number, y: Number, z: Number, amplitude: Double = 1.0): Double {
  val randomNumber = (1.0 + GetNoise(x.toDouble(), y.toDouble(), z.toDouble())) / 2.0
  return randomNumber * amplitude
}
