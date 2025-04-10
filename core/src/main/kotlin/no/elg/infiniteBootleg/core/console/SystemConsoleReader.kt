package no.elg.infiniteBootleg.core.console

import com.badlogic.gdx.utils.Disposable
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.util.launchOnMain
import java.io.Console
import java.util.Scanner

private val logger = KotlinLogging.logger {}

/** Read input from [System.console] or [System. in] if no console exists.  */
class SystemConsoleReader(private val consoleHandler: GameConsoleHandler) :
  Runnable,
  Disposable {
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
          launchOnMain { consoleHandler.execCommand(read) }
        } catch (e: Exception) {
          logger.error(e) { "Console reader closed due to exception" }
          dispose()
        }
      }
    }
  }

  override fun dispose() {
    running = false
  }
}
