package no.elg.infiniteBootleg.core.console

import com.badlogic.gdx.utils.Disposable
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.util.launchOnMainSuspendable
import java.io.Console
import java.util.Scanner

private val logger = KotlinLogging.logger {}

/** Read input from [System.console] or [System. in] if no console exists.  */
class SystemConsoleReader(private val consoleHandler: GameConsoleHandler) : Disposable {

  private var running = false
  private var started = false

  fun start() {
    require(!started) { "cannot start SystemConsoleReader twice" }
    started = true
    Thread.ofPlatform().daemon().name("System Console Reader Thread").start {
      running = true
      val scanner = System.console()?.run(Console::reader)?.let(::Scanner) ?: Scanner(System.`in`)
      scanner.use { openScanner ->
        while (running) {
          try {
            if (openScanner.hasNextLine()) {
              val read = openScanner.nextLine()
              launchOnMainSuspendable { consoleHandler.execCommand(read) }
            }
          } catch (e: Exception) {
            logger.error(e) { "Console reader closed due to exception" }
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
