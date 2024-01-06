package no.elg.infiniteBootleg.util

const val RADIANS_TO_DEGREES: Float = 57.29578f
const val DEGREES_TO_RADIANS: Float = 0.017453292f

typealias Degrees = Float
typealias Radians = Float

inline fun Radians.toDegrees(): Degrees = this * RADIANS_TO_DEGREES
inline fun Degrees.toRadians(): Radians = this * DEGREES_TO_RADIANS
