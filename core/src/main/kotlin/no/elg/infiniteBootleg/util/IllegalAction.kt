package no.elg.infiniteBootleg.util

import no.elg.infiniteBootleg.main.Main

enum class IllegalAction {
  /**
   * Throw an exception
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

  fun handle(tag: String = "IllegalAction", message: () -> String) {
    when (this) {
      THROW -> throw RuntimeException(message())
      STACKTRACE -> Main.logger().warn(tag, "${message()}\n${stacktrace()}")
      LOG -> Main.logger().warn(tag, message())
    }
  }
}
