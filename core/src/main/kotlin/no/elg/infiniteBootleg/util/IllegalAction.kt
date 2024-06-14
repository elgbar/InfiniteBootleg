package no.elg.infiniteBootleg.util

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.screens.MainMenuScreen
import kotlin.system.exitProcess

enum class IllegalAction {

  /**
   * Throw an uncatchable error, this will crash the game
   */
  CRASH,

  /**
   * Return to the main menu, handles as a [CRASH] if the game is running as the server
   */
  TO_MAIN_MENU,

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

  @Suppress("NOTHING_TO_INLINE") //want inline to get the correct logger
  inline fun handle(cause: Throwable? = null, noinline message: () -> String) {
    when (this) {
      THROW -> throw Error(message(), cause)
      STACKTRACE -> KotlinLogging.logger {}.warn(cause) { "${message()}\n${stacktrace()}" }
      LOG -> KotlinLogging.logger {}.warn(cause, message)
      CRASH -> {
        KotlinLogging.logger {}.error(cause, message)
        exitProcess(333)
      }

      TO_MAIN_MENU -> {
        if (Main.isServer) {
          KotlinLogging.logger {}.error(cause, message)
          exitProcess(334)
        } else {
          KotlinLogging.logger {}.error(cause, message)
          Main.inst().scheduler.executeSync { ClientMain.inst().screen = MainMenuScreen }
        }
      }
    }
  }
}
