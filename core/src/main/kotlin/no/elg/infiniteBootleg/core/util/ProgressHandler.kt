package no.elg.infiniteBootleg.core.util

import com.badlogic.gdx.math.Interpolation

data class ProgressHandler(
  private val durationSeconds: Float,
  private val interpolation: Interpolation = Interpolation.smooth,
  private val start: Float = 0.75f,
  private val end: Float = 0f
) {

  private var timeToTarget: Float = durationSeconds

  fun updateAndGetProgress(delta: Float): Float {
    update(delta)
    return progress
  }

  /**
   * @return True if the progress is just done
   */
  fun update(delta: Float): Boolean {
    val wasDone = isDone()
    timeToTarget -= delta
    return !wasDone && isDone()
  }

  /**
   * @return Progress between [start] and [end] using [interpolation]
   */
  val progress: Float
    get() {
      val progress: Float = if (timeToTarget <= 0) 1f else 1f - (timeToTarget / durationSeconds)
      return interpolation.apply(start, end, progress)
    }

  fun reset() {
    timeToTarget = durationSeconds
  }

  fun isDone(): Boolean = timeToTarget <= 0f
}
