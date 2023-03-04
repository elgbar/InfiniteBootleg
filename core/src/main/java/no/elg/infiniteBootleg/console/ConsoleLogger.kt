package no.elg.infiniteBootleg.console

import com.badlogic.gdx.ApplicationLogger
import com.strongjoshua.console.LogLevel

/**
 * @author Elg
 */
@JvmDefaultWithoutCompatibility
interface ConsoleLogger : ApplicationLogger {
  /**
   * Log a level with [LogLevel.DEFAULT] loglevel
   *
   * @param msg The message to log
   * @param objs The object to format
   * @see String.format
   */
  fun logf(msg: String, vararg objs: Any?) {
    logf(LogLevel.DEFAULT, msg, *objs)
  }

  /**
   * @param level The level to log at
   * @param msg The message to log
   * @param objs The object to format
   * @see String.format
   */
  fun logf(level: LogLevel, msg: String, vararg objs: Any?) {
    log(level, String.format(msg, *objs))
  }

  /**
   * @param level The level to log at
   * @param msg The message to log
   */
  fun log(level: LogLevel, msg: String)
  fun success(msg: String) {
    log(LogLevel.SUCCESS, msg)
  }

  /**
   * Log a level with [LogLevel.DEFAULT] loglevel
   *
   * @param msg The message to log
   */
  fun log(msg: String) {
    log(LogLevel.DEFAULT, msg)
  }

  fun success(msg: String, vararg objs: Any?) {
    logf(LogLevel.SUCCESS, msg, *objs)
  }

  override fun log(tag: String, message: String) {
    log("[$tag] $message")
  }

  fun warn(message: String) {
    error("WARN", message)
  }

  override fun log(tag: String, message: String, exception: Throwable?) {
    log(tag, message)
    exception?.printStackTrace(System.out)
  }

  fun info(tag: String, message: () -> String) {
    log(LogLevel.DEFAULT, "[$tag] ${message.invoke()}")
  }

  fun warn(message: String, vararg objs: Any?) {
    error("WARN", message, *objs)
  }

  fun error(tag: String, message: String, vararg objs: Any?) {
    logf(LogLevel.ERROR, "[$tag] $message", *objs)
  }

  fun warn(tag: String, message: String, vararg objs: Any?) {
    logf(LogLevel.ERROR, "WARN [$tag] $message", *objs)
  }

  fun error(message: String) {
    log(LogLevel.ERROR, message)
  }

  override fun error(tag: String, message: String) {
    log(LogLevel.ERROR, "[$tag] $message")
  }

  override fun error(tag: String, message: String, exception: Throwable) {
    error(tag, message)
    exception.printStackTrace(System.err)
  }

  override fun debug(tag: String, message: String) {
    log(LogLevel.DEFAULT, "DBG [$tag] $message")
  }

  fun debug(tag: String, message: () -> String) {
    log(LogLevel.DEFAULT, "DBG [$tag] ${message.invoke()}")
  }

  override fun debug(tag: String, message: String, exception: Throwable?) {
    debug(tag, message)
    exception?.printStackTrace(System.out)
  }
}
