package no.elg.infiniteBootleg.util

import no.elg.infiniteBootleg.world.generator.noise.FastNoiseLite

/**
 * @return Noise output bounded between 0..1
 */
fun FastNoiseLite.getNoise(x: Double, y: Double): Float {
  return (1 + GetNoise(x, y)) / 2f
}

/**
 * @return Noise output bounded between 0..1
 */
fun FastNoiseLite.getNoise(x: Double, y: Double, z: Double): Float {
  return (1 + GetNoise(x, y, z)) / 2f
}
