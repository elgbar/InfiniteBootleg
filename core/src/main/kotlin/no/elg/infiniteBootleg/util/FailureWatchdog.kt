package no.elg.infiniteBootleg.util

import no.elg.infiniteBootleg.main.Main

class FailureWatchdog(val failureMessage: String) {

  var renderFailuresInARow: Int = 0

  inline fun watch(runnable: () -> Unit) {
    try {
      runnable()
    } catch (e: Exception) {
      renderFailuresInARow++
      if (renderFailuresInARow >= MAX_FAILURES_IN_A_ROW) {
        throw RuntimeException("Failed to $failureMessage $MAX_FAILURES_IN_A_ROW times in a row", e)
      } else {
        Main.logger().warn("Failed to $failureMessage (currently on failure $renderFailuresInARow/$MAX_FAILURES_IN_A_ROW)", e)
      }
    }
  }

  companion object {
    const val MAX_FAILURES_IN_A_ROW = 5
  }
}
