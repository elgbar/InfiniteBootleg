package no.elg.infiniteBootleg.client.logging

import com.strongjoshua.console.LogLevel
import no.elg.infiniteBootleg.client.console.InGameConsoleHandler
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.core.Settings
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.Appender
import org.apache.logging.log4j.core.Core
import org.apache.logging.log4j.core.ErrorHandler
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginFactory

/**
 * An appender that logs to the in game console. If the package changes so must `package` in `log4j2.xml`
 */
@Plugin(name = "InGameConsole", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
class InGameConsoleAppender(name: String) : AbstractAppender(name, null, null, true, null) {

  private val console: InGameConsoleHandler by lazy { ClientMain.inst().console }

  init {
    // Ignore exceptions in this appender
    handler = object : ErrorHandler {
      override fun error(msg: String?) = error(msg, null, null)
      override fun error(msg: String?, t: Throwable?) = error(msg, null, t)
      override fun error(msg: String?, event: LogEvent?, t: Throwable?) {
        if (Settings.debug) {
          t?.printStackTrace()
        }
      }
    }
  }

  override fun append(event: LogEvent) {
    when (event.level) {
      Level.ERROR, Level.FATAL -> console.log(LogLevel.ERROR, event.message.toString())
      Level.WARN -> console.log(LogLevel.ERROR, "WARN ${event.message}")
      Level.INFO -> console.log(LogLevel.DEFAULT, event.message.toString())
    }
  }

  companion object {
    @Suppress("unused")
    @JvmStatic
    @PluginFactory
    fun createAppender(@PluginAttribute("name") name: String): InGameConsoleAppender = InGameConsoleAppender(name)
  }
}
