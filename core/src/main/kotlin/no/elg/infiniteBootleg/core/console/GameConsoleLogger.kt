package no.elg.infiniteBootleg.core.console

import com.badlogic.gdx.ApplicationLogger
import com.strongjoshua.console.LogLevel
import no.elg.infiniteBootleg.core.Settings

/**
 * @author Elg
 */
interface GameConsoleLogger : ApplicationLogger {

  /**
   * @param level The level to log at
   * @param msg The message to log
   */
  fun log(level: LogLevel, msg: String)

  override fun log(tag: String, message: String) = log(LogLevel.DEFAULT, "[$tag] $message")

  override fun log(tag: String, message: String, exception: Throwable?) {
    log(tag, message)
    exception?.printStackTrace(System.out)
  }

  override fun error(tag: String, message: String) {
    log(LogLevel.ERROR, "[$tag] $message")
  }

  override fun error(tag: String, message: String, exception: Throwable) {
    error(tag, message)
    exception.printStackTrace(System.err)
  }

  override fun debug(tag: String, message: String) {
    if (Settings.debug) {
      log(LogLevel.DEFAULT, "$DEBUG_PREFIX [$tag] $message")
    }
  }

  override fun debug(tag: String, message: String, exception: Throwable?) {
    if (Settings.debug) {
      log(LogLevel.DEFAULT, "$DEBUG_PREFIX [$tag] $message")
      exception?.printStackTrace(System.out)
    }
  }

  companion object {
    const val DEBUG_PREFIX = "DBG"
  }
}
