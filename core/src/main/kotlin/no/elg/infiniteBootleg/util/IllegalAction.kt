package no.elg.infiniteBootleg.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.system.exitProcess

enum class IllegalAction {

  /**
   * Exit the game with a crash message
   */
  CRASH,

  /**
   * Throw a runtime exception
   */
  THROW,

  /**
   * Print a stacktrace and log a warning
   */
  STACKTRACE,

  /**
   * Log a warning
   */
  LOG;

  @Suppress("NOTHING_TO_INLINE") // want inline to get the correct logger
  inline fun handle(cause: Throwable? = null, noinline message: () -> String) {
    val logger = KotlinLogging.logger {}
    when (this) {
      THROW -> throw RuntimeException(message(), cause)
      STACKTRACE -> logger.warn(cause) { "${message()}\n${stacktrace()}" }
      LOG -> logger.warn(cause, message)
      CRASH -> {
        logger.error(cause, message)
        exitProcess(333)
      }
    }
  }
}
