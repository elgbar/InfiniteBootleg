package no.elg.infiniteBootleg.console.consoles

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisWindow
import com.strongjoshua.console.GUIConsole
import no.elg.infiniteBootleg.console.ConsoleHandler
import no.elg.infiniteBootleg.console.HelpfulConsoleHelpUtil

class CGUIConsole(private val consoleHandler: ConsoleHandler, skin: Skin, useMultiplexer: Boolean, keyID: Int) : GUIConsole(
  skin,
  useMultiplexer,
  keyID,
  VisWindow::class.java,
  VisTable::class.java,
  "default-pane",
  TextField::class.java,
  VisTextButton::class.java,
  VisLabel::class.java,
  VisScrollPane::class.java
) {
  override fun printHelp(command: String) {
    HelpfulConsoleHelpUtil.printHelp(this, exec, command)
  }

  override fun printCommands() {
    HelpfulConsoleHelpUtil.printCommands(this, exec)
  }

  override fun execCommand(command: String) {
    consoleHandler.execCommand(command)
  }
}