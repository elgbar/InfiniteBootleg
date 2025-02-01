package no.elg.infiniteBootleg.client

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.client.args.ClientProgramArgs
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.defaultDisplayHeight
import no.elg.infiniteBootleg.core.util.defaultDisplayWidth
import java.time.Instant
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
  val startTime = Instant.now()
  val progArgs = ClientProgramArgs(args)
  Settings.client = true
  val main: Main = ClientMain(progArgs, startTime)
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
  if (Settings.debug) {
    config.enableGLDebugOutput(true, System.err)
  }
  config.setTitle(title)
  config.setBackBufferConfig(8, 8, 8, 8, 0, 0, 16)
  config.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate.coerceAtMost(Settings.foregroundFPS))
  config.setWindowIcon("textures/icon_64.png")
  config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL32, 4, 2)

  try {
    Lwjgl3Application(main, config)
  } catch (e: Exception) {
    logger.error(e) { "Uh-oh, unhandled exception caught!" }
    exitProcess(1)
  }
}
