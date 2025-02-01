package no.elg.infiniteBootleg.console

import com.strongjoshua.console.CommandExecutor
import com.strongjoshua.console.Console
import no.elg.infiniteBootleg.console.commands.CommonCommands
import no.elg.infiniteBootleg.console.consoles.StdConsole

class HeadlessGameConsoleHandler : GameConsoleHandler() {
  override val exec: CommandExecutor
    get() = CommonCommands()

  override val console: Console = StdConsole().apply {
    setConsoleStackTrace(true)
    setLoggingToSystem(true)
  }

  override var openConsoleKey: Int
    get() = Int.MIN_VALUE
    set(_) = Unit

  override var alpha: Float
    get() = 1f
    set(_) = Unit
}
