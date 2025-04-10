package no.elg.infiniteBootleg.core.main

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.Application
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Collections
import com.strongjoshua.console.CommandExecutor
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.async.KtxAsync
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.args.ProgramArgs
import no.elg.infiniteBootleg.core.console.GameConsoleHandler
import no.elg.infiniteBootleg.core.logging.Slf4jApplicationLogger
import no.elg.infiniteBootleg.core.util.Util
import no.elg.infiniteBootleg.core.util.diffTimePretty
import no.elg.infiniteBootleg.core.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.core.world.generator.chunk.ChunkFactory
import no.elg.infiniteBootleg.core.world.generator.chunk.ChunkImplFactory
import org.fusesource.jansi.AnsiConsole
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
abstract class CommonMain<CONSOLE : GameConsoleHandler>(private val progArgs: ProgramArgs, override val startTime: Instant) :
  ApplicationAdapter(),
  Main {
  final override lateinit var console: CONSOLE
    protected set

  open val exec: CommandExecutor get() = console.exec

  final override lateinit var renderThreadName: String
    private set

  override val chunkFactory: ChunkFactory<out ChunkImpl> = ChunkImplFactory()

  init {
    instField = this
  }

  abstract fun createConsole(): CONSOLE

  override fun create() {
    AnsiConsole.systemInstall()
    KtxAsync.initiate()
    renderThreadName = Thread.currentThread().name
    console = createConsole().apply {
      alpha = 0.85f
      create()
    }
    Gdx.app.applicationLogger = Slf4jApplicationLogger()
    Gdx.app.logLevel = if (Settings.debug) Application.LOG_DEBUG else Application.LOG_INFO

    if (!AnsiConsole.isInstalled()) {
      logger.warn { "Failed to install jansi" }
    }
    logger.info { "Version ${Util.version}" }

    Runtime.getRuntime().addShutdownHook(
      Thread {
        if (Main.Companion.isAuthoritative) {
          val activeWorld = world
          activeWorld?.save()
          activeWorld?.dispose()
        }
      }
    )
  }

  protected fun afterCreate() {
    progArgs.onCreate()
    Util.getLastCommitDate(Util.RELATIVE_TIME)?.also {
      logger.debug { "Last commit created $it" }
    }
    logger.info { "You can also start the program with arguments for '--help' or '-?' as arg to see all possible options" }
    logger.info {
      val processStartupTime = ProcessHandle.current().info().startInstant().map(::diffTimePretty).orElseGet { "???" }
      val userStartupTime = diffTimePretty(startTime)
      "Create in $userStartupTime (process: $processStartupTime)"
    }
  }

  override val engine: Engine? get() = world?.engine

  override fun dispose() {
    console.dispose()
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
