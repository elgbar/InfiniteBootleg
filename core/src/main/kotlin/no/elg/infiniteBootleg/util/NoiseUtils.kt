package no.elg.infiniteBootleg.util

import no.elg.infiniteBootleg.world.generator.noise.FastNoiseLite

/**
 * @return Noise output bounded between 0..[amplitude]
 */
fun FastNoiseLite.getNoise(x: Number, y: Number, amplitude: Float = 1f): Float {
  val randomNumber = (1.0 + GetNoise(x.toDouble(), y.toDouble())) / 2.0
  return (randomNumber * amplitude.toDouble()).toFloat()
}

/**
 * @return Noise output bounded between 0..1
 */
fun FastNoiseLite.getNoise(x: Number, y: Number, z: Number, amplitude: Float = 1f): Float {
  val randomNumber = (1 + GetNoise(x.toDouble(), y.toDouble(), z.toDouble())) / 2f
  return (randomNumber * amplitude.toDouble()).toFloat()
}
