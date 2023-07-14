package no.elg.infiniteBootleg.console

import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.Main
import java.util.Scanner

/** Read input from [System.console] or [System. in] if no console exists.  */
class SystemConsoleReader(private val consoleHandler: ConsoleHandler) : Runnable, Disposable {
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
    val console = System.console()
    if (console != null) {
      Scanner(console.reader())
    } else {
      Scanner(System.`in`).use { scanner ->
        while (running) {
          var read: String?
          try {
            read = scanner.nextLine()
            Main.inst().scheduler.executeSync { consoleHandler.execCommand(read) }
          } catch (e: Exception) {
            dispose()
          }
        }
      }
    }
  }

  override fun dispose() {
    running = false
  }
}
