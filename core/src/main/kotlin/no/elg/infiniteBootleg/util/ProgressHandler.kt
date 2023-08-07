package no.elg.infiniteBootleg.util

import com.badlogic.gdx.math.Interpolation

data class ProgressHandler(
  val durationSeconds: Float,
  private val interpolation: Interpolation = Interpolation.smooth,
  private val start: Float = 0.75f,
  private val end: Float = 0f
) {

  private var timeToTarget = durationSeconds

  /**
   * @return Alpha between 1f and 0f
   */
  fun calculateProgress(delta: Float): Float {
    timeToTarget -= delta
    val progress: Float = if (timeToTarget < 0) 1f else 1f - timeToTarget / durationSeconds
    return interpolation.apply(start, end, progress)
  }

  fun reset() {
    timeToTarget = durationSeconds
  }

  fun isDone(): Boolean = timeToTarget <= 0f
}
