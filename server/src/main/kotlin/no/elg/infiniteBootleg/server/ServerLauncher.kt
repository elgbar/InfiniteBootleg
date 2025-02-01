package no.elg.infiniteBootleg.server

import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.server.args.ServerProgramArgs
import no.elg.infiniteBootleg.world.ticker.TickerImpl.Companion.DEFAULT_TICKS_PER_SECOND
import java.time.Instant

fun main(args: Array<String>) {
  val startTime = Instant.now()
  val progArgs = ServerProgramArgs(args)
  Settings.client = false
  val config = HeadlessApplicationConfiguration()
  config.updatesPerSecond = (if (Settings.tps < 0) DEFAULT_TICKS_PER_SECOND else Settings.tps).toInt()
  val main: Main = ServerMain(progArgs, startTime)
  HeadlessApplication(main, config)
}
