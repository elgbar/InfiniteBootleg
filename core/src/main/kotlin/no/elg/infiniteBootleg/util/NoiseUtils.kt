package no.elg.infiniteBootleg.util

import no.elg.infiniteBootleg.world.generator.noise.FastNoiseLite

/**
 * @return Noise output bounded between 0..1
 */
fun FastNoiseLite.getNoise(x: Number, y: Number): Float = (1 + GetNoise(x.toDouble(), y.toDouble())) / 2f

/**
 * @return Noise output bounded between 0..1
 */
fun FastNoiseLite.getNoise(x: Number, y: Number, z: Number): Float = (1 + GetNoise(x.toDouble(), y.toDouble(), z.toDouble())) / 2f
