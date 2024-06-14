package no.elg.infiniteBootleg.console

import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.main.Main
import java.io.Console
import java.util.Scanner

/** Read input from [System.console] or [System. in] if no console exists.  */
class SystemConsoleReader(private val consoleHandler: InGameConsoleHandler) : Runnable, Disposable {
  private var running = false
  fun start() {
    val thread = Thread(this, "System Console Reader Thread")
    thread.isDaemon = true
    thread.start()
  }

  override fun run() {
    if (running) {
      return
    }
    running = true
    val scanner = System.console()?.run(Console::reader)?.let(::Scanner) ?: Scanner(System.`in`)
    scanner.use { openScanner ->
      while (running) {
        var read: String?
        try {
          read = openScanner.nextLine()
          Main.inst().scheduler.executeSync { consoleHandler.execCommand(read) }
        } catch (e: Exception) {
          dispose()
        }
      }
    }
  }

  override fun dispose() {
    running = false
  }
}
