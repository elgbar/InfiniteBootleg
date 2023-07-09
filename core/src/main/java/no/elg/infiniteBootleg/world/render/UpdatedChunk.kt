package no.elg.infiniteBootleg.world.render

import com.badlogic.gdx.math.Interpolation
import no.elg.infiniteBootleg.world.Chunk

data class UpdatedChunk(val chunk: Chunk, val durationSeconds: Float = 0.2f) {

  private var timeToTarget = durationSeconds

  /**
   * @return Alpha between 1f and 0f
   */
  fun calculateAlpha(delta: Float): Float {
    timeToTarget -= delta
    val progress: Float = if (timeToTarget < 0) 1f else 1f - timeToTarget / durationSeconds
    return Interpolation.smooth.apply(1f, 0f, progress)
  }

  fun isDone(): Boolean = timeToTarget <= 0f
}
