package no.elg.infiniteBootleg.core.util

import io.github.oshai.kotlinlogging.KotlinLogging

class FailureWatchdog(val failureMessage: String, val illegalAction: IllegalAction = IllegalAction.CRASH) {

  var failuresInARow: Int = 0

  inline fun watch(source: CheckableDisposable, crossinline runnable: () -> Unit) {
    if (source.isDisposed) {
      watchdogLogger.debug { "Will not execute runnable as the source is disposed" }
      return
    }
    try {
      runnable()
      failuresInARow = 0
    } catch (e: Throwable) {
      if (source.isDisposed) {
        if (watchdogLogger.isDebugEnabled()) {
          watchdogLogger.debug(e) { "Will not react to failure as the source is disposed" }
        } else {
          watchdogLogger.info { "Will not react to failure as the source is disposed" }
        }
        return
      }
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
