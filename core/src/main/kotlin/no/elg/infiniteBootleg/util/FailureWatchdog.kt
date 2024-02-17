package no.elg.infiniteBootleg.util

import no.elg.infiniteBootleg.main.Main

class FailureWatchdog(val failureMessage: String, val illegalAction: IllegalAction = IllegalAction.TO_MAIN_MENU) {

  var failuresInARow: Int = 0

  inline fun watch(runnable: () -> Unit) {
    try {
      runnable()
      failuresInARow = 0
    } catch (e: Exception) {
      failuresInARow++
      if (failuresInARow >= MAX_FAILURES_IN_A_ROW) {
        illegalAction.handle("FailureWatchdog", e) { "Failed to $failureMessage $MAX_FAILURES_IN_A_ROW times in a row" }
      } else {
        Main.logger().warn("Failed to $failureMessage (currently on failure $failuresInARow/$MAX_FAILURES_IN_A_ROW)", e)
      }
    }
  }

  companion object {
    const val MAX_FAILURES_IN_A_ROW = 5
  }
}
