package no.elg.infiniteBootleg.world.render.debug

import com.badlogic.gdx.math.Interpolation

data class VisualizeUpdate(val durationSeconds: Float) {

  private var timeToTarget = durationSeconds

  /**
   * @return Alpha between 1f and 0f
   */
  fun calculateAlpha(delta: Float): Float {
    timeToTarget -= delta
    val progress: Float = if (timeToTarget < 0) 1f else 1f - timeToTarget / durationSeconds
    return Interpolation.smooth.apply(0.75f, 0f, progress)
  }

  fun isDone(): Boolean = timeToTarget <= 0f
}