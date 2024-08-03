package no.elg.infiniteBootleg.main

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Application
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Collections
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.args.ProgramArgs
import no.elg.infiniteBootleg.assets.InfAssets
import no.elg.infiniteBootleg.assets.InfAssetsImpl
import no.elg.infiniteBootleg.console.InGameConsoleHandler
import no.elg.infiniteBootleg.logging.Slf4jApplicationLogger
import no.elg.infiniteBootleg.main.Main.Companion.isAuthoritative
import no.elg.infiniteBootleg.util.CancellableThreadScheduler
import no.elg.infiniteBootleg.util.Util

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
abstract class CommonMain(progArgs: ProgramArgs) : ApplicationAdapter(), Main {
  override val scheduler: CancellableThreadScheduler = CancellableThreadScheduler(Settings.schedulerThreads)

  override lateinit var console: InGameConsoleHandler
    protected set

  override val assets: InfAssets = InfAssetsImpl()

  init {
    progArgs.dispose()
    instField = this
  }

  override fun create() {
    console = InGameConsoleHandler()
    Gdx.app.applicationLogger = Slf4jApplicationLogger()
    Gdx.app.logLevel = if (Settings.debug) Application.LOG_DEBUG else Application.LOG_INFO
    console.alpha = 0.85f
    assets.loadAssets()
    logger.info { "Version ${Util.getVersion()}" }
    Util.getLastCommitDate(Util.RELATIVE_TIME)?.also {
      logger.debug { "Last commit created $it" }
    }
    logger.info { "You can also start the program with arguments for '--help' or '-?' as arg to see all possible options" }
  }

  override val engine: Engine? get() = world?.engine

  override fun isAuthorizedToChange(entity: Entity): Boolean = isAuthoritative

  override fun dispose() {
    console.dispose()
    scheduler.shutdown()
  }

  companion object {

    private val INST_LOCK = Any()

    /**
     * Backing nullable field of main
     */
    @JvmStatic
    var instField: Main? = null
      private set(value) {
        synchronized(INST_LOCK) {
          check(field == null) { "A common main instance have already be declared" }
          field = value
        }
      }

    init {
      Collections.allocateIterators = true
    }
  }
}
