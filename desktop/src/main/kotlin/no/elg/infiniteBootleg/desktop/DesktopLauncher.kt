package no.elg.infiniteBootleg.desktop

import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.args.ProgramArgs
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.main.ServerMain
import no.elg.infiniteBootleg.util.defaultDisplayHeight
import no.elg.infiniteBootleg.util.defaultDisplayWidth
import no.elg.infiniteBootleg.world.ticker.TickerImpl.Companion.DEFAULT_TICKS_PER_SECOND
import org.fusesource.jansi.AnsiConsole
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
  AnsiConsole.systemInstall()
  if (!AnsiConsole.isInstalled()) {
    logger.warn { "Failed to install jansi" }
  }

  val progArgs = ProgramArgs(args)
  if (Settings.client) {
    val main: Main = ClientMain(false, progArgs)
    val config = Lwjgl3ApplicationConfiguration()
    config.disableAudio(true)
    config.setWindowedMode(defaultDisplayWidth / 2, defaultDisplayHeight / 2)
    config.useVsync(Settings.vsync)
    var title = "Infinite Terraria "
    when {
      Settings.debug -> title += "(debug) "
      Settings.ignoreWorldLock -> title += "dangerous! "
      !Settings.loadWorldFromDisk -> title += "transient "
    }
    config.setTitle(title)
    config.setBackBufferConfig(8, 8, 8, 8, 0, 0, 16)
    config.setForegroundFPS(Settings.foregroundFPS)
    config.setWindowIcon("textures/icon_64.png")
    config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL32, 4, 2)

    try {
      Lwjgl3Application(main, config)
    } catch (e: Exception) {
      logger.error(e) { "Uh-oh, unhandled exception caught!" }
      exitProcess(1)
    }
  } else {
    val config = HeadlessApplicationConfiguration()
    config.updatesPerSecond = (if (Settings.tps < 0) DEFAULT_TICKS_PER_SECOND else Settings.tps).toInt()
    val main: Main = ServerMain(false, progArgs)
    HeadlessApplication(main, config)
  }
}
