package no.elg.infiniteBootleg.main

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Application
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Collections
import com.strongjoshua.console.LogLevel
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.args.ProgramArgs
import no.elg.infiniteBootleg.assets.InfAssets
import no.elg.infiniteBootleg.assets.InfAssetsImpl
import no.elg.infiniteBootleg.console.ConsoleHandler
import no.elg.infiniteBootleg.console.ConsoleLogger
import no.elg.infiniteBootleg.main.Main.Companion.isAuthoritative
import no.elg.infiniteBootleg.util.CancellableThreadScheduler
import no.elg.infiniteBootleg.util.Util

/**
 * @author Elg
 */
abstract class CommonMain protected constructor(protected val test: Boolean, progArgs: ProgramArgs?) : ApplicationAdapter(), Main {
  override val scheduler: CancellableThreadScheduler = CancellableThreadScheduler(Settings.schedulerThreads)

  override lateinit var console: ConsoleHandler
    protected set

  override val assets: InfAssets = InfAssetsImpl()

  init {
    progArgs?.dispose()
    synchronized(Main.INST_LOCK) {
      check(Companion::instField.isLateinit) { "A common main instance have already be declared" }
      instField = this
    }
  }

  override fun create() {
    console = ConsoleHandler()
    Gdx.app.applicationLogger = console
    Gdx.app.logLevel = if (test || Settings.debug) Application.LOG_DEBUG else Application.LOG_INFO
    console.alpha = 0.85f
    assets.loadAssets()
    console.log(LogLevel.SUCCESS, "Version ${Util.getVersion()}")
    Util.getLastCommitDate("relative")?.also {
      console.log(LogLevel.SUCCESS, "Last commit created $it")
    }
    console.log("You can also start the program with arguments for '--help' or '-?' as arg to see all possible options")
  }

  override val engine: Engine? get() = world?.engine
  override val isNotTest: Boolean get() = !test
  override val consoleLogger: ConsoleLogger get() = console

  override fun isAuthorizedToChange(entity: Entity): Boolean = isAuthoritative

  override fun dispose() {
    console.dispose()
    scheduler.shutdown()
  }

  companion object {
    lateinit var instField: Main
      private set

    init {
      Collections.allocateIterators = true
    }
  }
}
