package no.elg.infiniteBootleg.client.logging

import com.strongjoshua.console.LogLevel
import no.elg.infiniteBootleg.core.main.Main
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.Appender
import org.apache.logging.log4j.core.Core
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginFactory

@Plugin(name = "InGameConsole", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
class InGameConsoleAppender(name: String) : AbstractAppender(name, null, null, true, null) {

  override fun append(event: LogEvent) {
    val console = Main.Companion.inst().console
    when (event.level) {
      Level.ERROR, Level.FATAL -> console.log(LogLevel.ERROR, event.message.toString())
      Level.WARN -> console.log(LogLevel.ERROR, "WARN ${event.message}")
      Level.INFO -> console.log(LogLevel.DEFAULT, event.message.toString())
    }
  }

  companion object {
    @JvmStatic
    @PluginFactory
    fun createAppender(@PluginAttribute("name") name: String): InGameConsoleAppender = InGameConsoleAppender(name)
  }
}
