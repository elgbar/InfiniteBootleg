package no.elg.infiniteBootleg.util

import io.github.oshai.kotlinlogging.KotlinLogging

class FailureWatchdog(val failureMessage: String, val illegalAction: IllegalAction = IllegalAction.CRASH) {

  var failuresInARow: Int = 0

  inline fun watch(crossinline runnable: () -> Unit) {
    try {
      runnable()
      failuresInARow = 0
    } catch (e: Throwable) {
      failuresInARow++
      if (failuresInARow >= MAX_FAILURES_IN_A_ROW) {
        illegalAction.handle(e) { "Failed to $failureMessage $MAX_FAILURES_IN_A_ROW times in a row" }
      } else {
        watchdogLogger.warn(e) { "Error when watching $failureMessage" }
      }
    }
  }

  companion object {
    val watchdogLogger = KotlinLogging.logger {} // logger must be here to avoid conflicting logger
    const val MAX_FAILURES_IN_A_ROW = 5
  }
}
