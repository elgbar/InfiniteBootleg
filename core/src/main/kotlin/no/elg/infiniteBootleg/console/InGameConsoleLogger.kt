package no.elg.infiniteBootleg.console

import com.badlogic.gdx.ApplicationLogger
import com.strongjoshua.console.LogLevel
import no.elg.infiniteBootleg.Settings

/**
 * @author Elg
 */
@JvmDefaultWithoutCompatibility
interface ConsoleLogger : ApplicationLogger {

  /**
   * @param level The level to log at
   * @param msg The message to log
   */
  @Deprecated("Use standard slf4j logger")
  fun log(level: LogLevel, msg: String)

  /**
   * Log a level with [LogLevel.DEFAULT] loglevel
   *
   * @param msg The message to log
   */
  fun log(msg: String) {
    log(LogLevel.DEFAULT, msg)
  }

  override fun log(tag: String, message: String) {
    log("[$tag] $message")
  }

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
    debug(tag, message)
    exception?.printStackTrace(System.out)
  }

  companion object {
    const val DEBUG_PREFIX = "DBG"
  }
}
