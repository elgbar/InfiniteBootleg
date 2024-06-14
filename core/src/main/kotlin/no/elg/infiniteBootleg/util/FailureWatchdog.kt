package no.elg.infiniteBootleg.util

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class FailureWatchdog(private val failureMessage: String, private val illegalAction: IllegalAction = IllegalAction.TO_MAIN_MENU) {

  private var failuresInARow: Int = 0

  fun watch(runnable: () -> Unit) {
    try {
      runnable()
      failuresInARow = 0
    } catch (e: Throwable) {
      failuresInARow++
      if (failuresInARow >= MAX_FAILURES_IN_A_ROW) {
        illegalAction.handle(e) { "Failed to $failureMessage $MAX_FAILURES_IN_A_ROW times in a row" }
      } else {
        logger.warn(e) { "Error when watching $failureMessage" }
      }
    }
  }

  companion object {
    const val MAX_FAILURES_IN_A_ROW = 5
  }
}
