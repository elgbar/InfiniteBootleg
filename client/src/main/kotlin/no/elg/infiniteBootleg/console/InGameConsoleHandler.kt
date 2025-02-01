package no.elg.infiniteBootleg.console

import com.badlogic.gdx.Input
import com.kotcrab.vis.ui.VisUI
import com.strongjoshua.console.Console
import no.elg.infiniteBootleg.console.commands.ClientCommands
import no.elg.infiniteBootleg.console.consoles.InGameConsole
import no.elg.infiniteBootleg.main.ClientMain

class InGameConsoleHandler : GameConsoleHandler() {
  override val exec: ClientCommands = ClientCommands()

  override val console: Console = InGameConsole(this, VisUI.getSkin(), false, Input.Keys.APOSTROPHE).apply {
    setLoggingToSystem(false)
  }

  override var openConsoleKey: Int
    get() = console.displayKeyID
    set(value) {
      console.displayKeyID = value
    }

  override var alpha: Float
    get() = console.window.color.a
    set(a) {
      console.window.color.a = a
    }

  override fun addToInputMultiplexer() {
    ClientMain.inst().inputMultiplexer.addProcessor(console.inputProcessor)
  }
}
