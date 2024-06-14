package no.elg.infiniteBootleg.logging

import no.elg.infiniteBootleg.main.CommonMain
import org.apache.logging.log4j.Level.DEBUG
import org.apache.logging.log4j.Level.ERROR
import org.apache.logging.log4j.Level.FATAL
import org.apache.logging.log4j.Level.INFO
import org.apache.logging.log4j.Level.TRACE
import org.apache.logging.log4j.Level.WARN
import org.apache.logging.log4j.core.Appender
import org.apache.logging.log4j.core.Core
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginFactory

@Plugin(name = "InGameConsole", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
class InGameConsoleAppender(name: String, val excludeLoggerName: Boolean) : AbstractAppender(name, null, null, true, null) {

  override fun append(event: LogEvent) {
    val console = CommonMain.instField?.console ?: return
    if (console.inGameConsole) {
      if (excludeLoggerName) {
        when (event.level) {
          ERROR, FATAL -> console.error(event.message.toString())
          WARN -> console.warn(event.message.toString())
          INFO -> console.log(event.message.toString())
          DEBUG -> console.log("DEBUG", event.message.toString())
          TRACE -> console.log("TRACE", event.message.toString())
        }
      } else {
        when (event.level) {
          ERROR, FATAL -> console.error(event.loggerName, event.message.toString())
          WARN -> console.warn(event.loggerName, event.message.toString())
          INFO -> console.log(event.loggerName, event.message.toString())
          DEBUG -> console.debug(event.loggerName, event.message.toString())
          TRACE -> console.debug("TRACE ${event.loggerName}", event.message.toString())
        }
      }
    }
  }

  companion object {

    @JvmStatic
    @PluginFactory
    fun createAppender(@PluginAttribute("name") name: String, @PluginAttribute("excludeLoggerName") excludeLoggerName: Boolean): InGameConsoleAppender =
      InGameConsoleAppender(name, excludeLoggerName)
  }
}
