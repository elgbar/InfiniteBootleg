package no.elg.infiniteBootleg.core.util

import kotlin.math.max

const val JUMP_VERTICAL_VEL = 10f
const val FLY_VEL = 1f

const val MAX_X_VEL = 7.5f // ie target velocity
const val MAX_Y_VEL = 40f
val MAX_WORLD_VEL: Float = max(MAX_X_VEL, MAX_Y_VEL)

const val INITIAL_BRUSH_SIZE = 1f
const val INITIAL_INTERACT_RADIUS = 32f
const val INITIAL_INSTANT_BREAK = false
