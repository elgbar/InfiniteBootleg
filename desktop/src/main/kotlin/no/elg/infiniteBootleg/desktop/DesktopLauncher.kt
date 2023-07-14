package no.elg.infiniteBootleg.desktop

import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.args.ProgramArgs
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.main.ServerMain
import no.elg.infiniteBootleg.util.defaultDisplayHeight
import no.elg.infiniteBootleg.util.defaultDisplayWidth
import no.elg.infiniteBootleg.world.ticker.TickerImpl.Companion.DEFAULT_TICKS_PER_SECOND

fun main(args: Array<String>) {
  val progArgs = ProgramArgs(args)
  if (Settings.client) {
    val main: Main = ClientMain(false, progArgs)
    val config = Lwjgl3ApplicationConfiguration()
    config.disableAudio(true)
    config.setWindowedMode(defaultDisplayWidth / 2, defaultDisplayHeight / 2)
    config.useVsync(true)
    config.setTitle("Infinite Terraria")
    config.setBackBufferConfig(8, 8, 8, 8, 0, 0, 16)
    config.setForegroundFPS(361) // Max hz reasonably to expect
    Lwjgl3Application(main, config)
  } else {
    val config = HeadlessApplicationConfiguration()
    config.updatesPerSecond = (if (Settings.tps < 0) DEFAULT_TICKS_PER_SECOND else Settings.tps).toInt()
    val main: Main = ServerMain(false, progArgs)
    HeadlessApplication(main, config)
  }
}
