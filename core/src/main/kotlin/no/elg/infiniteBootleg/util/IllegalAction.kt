package no.elg.infiniteBootleg.util

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
   * Return to the main menu
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

  fun handle(tag: String = "IllegalAction", cause: Throwable? = null, message: () -> String) {
    when (this) {
      THROW -> throw Error(message(), cause)
      STACKTRACE -> Main.logger().warn(tag, "${message()}\n${stacktrace()}", cause)
      LOG -> Main.logger().warn(tag, message(), cause)
      CRASH -> {
        Main.logger().error(tag, message(), cause)
        exitProcess(333)
      }

      TO_MAIN_MENU -> {
        Main.logger().error(tag, message(), cause)
        Main.inst().scheduler.executeSync { ClientMain.inst().screen = MainMenuScreen }
      }
    }
  }
}
