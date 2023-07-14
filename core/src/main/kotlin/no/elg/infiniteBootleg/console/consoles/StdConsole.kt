package no.elg.infiniteBootleg.console.consoles

import com.strongjoshua.console.HeadlessConsole
import no.elg.infiniteBootleg.console.HelpfulConsoleHelpUtil

/** A console that reads input from standard in  */
class StdConsole : HeadlessConsole() {
  override fun printHelp(command: String) {
    HelpfulConsoleHelpUtil.printHelp(this, exec, command)
  }

  override fun printCommands() {
    HelpfulConsoleHelpUtil.printCommands(this, exec)
  }

  override fun isVisible(): Boolean = true
}
